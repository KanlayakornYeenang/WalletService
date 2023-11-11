package com.example.walletservice.controller;

import com.example.walletservice.data.Transaction;
import com.example.walletservice.data.Wallet;
import com.example.walletservice.data.WalletInfo;
import com.example.walletservice.data.user.User;
import com.example.walletservice.data.user.UserInfo;
import com.example.walletservice.rest.*;
import com.example.walletservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("api/wallets/")
public class WalletController {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JwtService jwtService;

    @GetMapping("user/{user_id}")
    public ResponseEntity<RestModel> serviceGetWallet(@RequestHeader(value = "Authorization", required = true) String authorizationHeader, @PathVariable("user_id") String user_id) {
        try {
            String[] token = authorizationHeader.split(" ");
            Claims claims = jwtService.parseToken(token[1]);
            String claimsUserId = claims.getSubject().toString();

            User foundUser = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + user_id, User.class);
            if (foundUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestModel.builder().message("user id not exist 🔴").build());
            }

            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", user_id);
            if (foundWallet == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(RestModel.builder().message("this user doesn't have wallet 🔴").build());
            }

            if (claimsUserId.equals(user_id)) {
                WalletRestModel model = WalletRestModel.builder().wallet(foundWallet).user(foundUser).build();
                return ResponseEntity.ok(RestModel.builder().data(model).message("get wallet successful 🟢").build());
            } else {
                UserInfo info = UserInfo.builder()
                        .first_name(foundUser.getInfo().getFirst_name())
                        .last_name(foundUser.getInfo().getLast_name())
                        .build();
                User user = User.builder()._id(user_id).info(info).build();
                return ResponseEntity.ok(RestModel.builder().data(user).message("get info successful 🟢").build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestModel.builder().message("get wallet failed 🔴").build());
        }
    }

    @Transactional
    @PostMapping("payment")
    public ResponseEntity<RestModel> serviceCreatePayment(@RequestHeader(value = "Authorization", required = true) String authorizationHeader, @RequestBody CreatePaymentRestModel model) {
        String[] token = authorizationHeader.split(" ");
        Claims claims = jwtService.parseToken(token[1]);
        String payer_id = claims.getSubject().toString();

        User payer = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + payer_id, User.class);
        if (payer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RestModel.builder().message("payer user id not exist 🔴").build());
        }

        Wallet payerWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", payer_id);
        if (payerWallet == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(RestModel.builder().message("payer doesn't have wallet 🔴").build());
        }

        Wallet payeeWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletById", model.getPay_to_wallet_id());
        if (payeeWallet == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(RestModel.builder().message("payee doesn't have wallet 🔴").build());
        }
        User payee = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + payeeWallet.getUser_id(), User.class);

        if (model.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(RestModel.builder().message("amount must be greater than 0 🔴").build());
        }
        if (model.getAmount().compareTo(payerWallet.getInfo().getBalance()) > 0) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(RestModel.builder().message("insufficient funds to complete the transaction 🔴").build());
        }

        BigDecimal payerBalance = payerWallet.getInfo().getBalance().subtract(model.getAmount());
        BigDecimal payeeBalance = payeeWallet.getInfo().getBalance().add(model.getAmount());

        WalletInfo payerInfo = payerWallet.getInfo();
        WalletInfo payeeInfo = payeeWallet.getInfo();

        payerInfo.setBalance(payerBalance);
        payeeInfo.setBalance(payeeBalance);

        Transaction transaction = Transaction.builder()
                .timestamp(LocalDateTime.now().toString())
                .payee_wallet_id(payeeWallet.get_id())
                .payer_wallet_id(payerWallet.get_id())
                .amount(model.getAmount())
                .description(model.getDescription())
                .build();
        Transaction saveTransaction = (Transaction) rabbitTemplate.convertSendAndReceive("TransactionDirect", "saveTransaction", transaction);

        List<String> payerTransactions = payerInfo.getTransactions();
        List<String> payeeTransactions = payeeInfo.getTransactions();
        payerTransactions.add(saveTransaction.get_id());
        payeeTransactions.add(saveTransaction.get_id());

        payerInfo.setTransactions(payerTransactions);
        payeeInfo.setTransactions(payeeTransactions);
        payerWallet.setInfo(payerInfo);
        payeeWallet.setInfo(payeeInfo);

        rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", payerWallet);
        rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", payeeWallet);

        TransactionRestModel transactionRestModel = TransactionRestModel.builder()
                ._id(saveTransaction.get_id())
                .payer(payer)
                .payee(payee)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .timestamp(transaction.getTimestamp())
                .build();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("transaction", transactionRestModel);

        return ResponseEntity.ok(RestModel.builder().data(responseData).message("payment successful 🟢").build());
    }

    @GetMapping("transactions/{transaction_id}")
    public ResponseEntity<RestModel> serviceGetTransactionById(@RequestHeader(value = "Authorization", required = true) String authorizationHeader, @PathVariable("transaction_id") String transaction_id) {
        try {
            String[] token = authorizationHeader.split(" ");
            Claims claims = jwtService.parseToken(token[1]);
            String user_id = claims.getSubject().toString();

            Wallet walletOwner = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", user_id);
            if (walletOwner == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(RestModel.builder().message("this user doesn't have wallet 🔴").build());
            }

            List<String> transactions = walletOwner.getInfo().getTransactions();
            if (transactions.contains(transaction_id)) {
                Transaction transaction = (Transaction) rabbitTemplate.convertSendAndReceive("TransactionDirect", "getTransactionById", transaction_id);

                String payer_id = null;
                if (transaction.getPayer_wallet_id() != null) {
                    payer_id = (String) rabbitTemplate.convertSendAndReceive("WalletDirect", "getUserIdByWalletId", transaction.getPayer_wallet_id());
                }
                String payee_id = (String) rabbitTemplate.convertSendAndReceive("WalletDirect", "getUserIdByWalletId", transaction.getPayee_wallet_id());

                User payer = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + payer_id, User.class);
                User payee = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + payee_id, User.class);

                TransactionRestModel transactionRestModel = TransactionRestModel.builder()
                        ._id(transaction_id)
                        .payer(payer)
                        .payee(payee)
                        .amount(transaction.getAmount())
                        .description(transaction.getDescription())
                        .timestamp(transaction.getTimestamp())
                        .build();
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("transaction", transactionRestModel);

                return ResponseEntity.ok(RestModel.builder().data(responseData).message("get transaction successful 🟢").build());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestModel.builder().message("transaction id not exist 🔴").build());
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestModel.builder().message("get transactions failed 🔴").build());
        }
    }

    @PostMapping("activate")
    public ResponseEntity<RestModel> serviceActivateWallet(@RequestBody Wallet wallet) {
        try {
            User foundUser = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + wallet.getUser_id(), User.class);
            if (foundUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestModel.builder().message("user id not exist 🔴").build());
            }

            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", wallet.getUser_id());
            if (foundWallet != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(RestModel.builder().message("this user already activate wallet 🔴").build());
            }

            Wallet createWallet = Wallet.builder().user_id(wallet.getUser_id()).info(new WalletInfo()).build();
            Wallet activatedWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", createWallet);
            WalletRestModel walletRestModel = WalletRestModel.builder().wallet(activatedWallet).user(foundUser).build();
            return ResponseEntity.ok(RestModel.builder().data(walletRestModel).message("activate wallet successful 🟢").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestModel.builder().message("activate wallet failed 🔴").build());
        }
    }

    @PostMapping("topUp")
    public ResponseEntity<RestModel> serviceTopUp(@RequestBody TopUpRestModel model) {
        try {
            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletById", model.getWallet_id());
            if (foundWallet == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestModel.builder().message("wallet id not exist 🔴").build());
            }
            User payee = new RestTemplate().getForObject("https://user2-908649839259189283.rcf2.deploys.app/api/user/" + foundWallet.getUser_id(), User.class);

            WalletInfo walletInfo = foundWallet.getInfo();
            BigDecimal balance = walletInfo.getBalance();
            walletInfo.setBalance(balance.add(model.getAmount()));

            Transaction saveTransaction = Transaction.builder()
                    .payee_wallet_id(model.getWallet_id())
                    .amount(model.getAmount())
                    .description(model.getDescription())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            saveTransaction = (Transaction) rabbitTemplate.convertSendAndReceive("TransactionDirect", "saveTransaction", saveTransaction);

            List<String> transactions = walletInfo.getTransactions();
            transactions.add(saveTransaction.get_id());
            walletInfo.setTransactions(transactions);
            foundWallet.setInfo(walletInfo);
            rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", foundWallet);

            TransactionRestModel transactionRestModel = TransactionRestModel.builder()
                    ._id(saveTransaction.get_id())
                    .payee(payee)
                    .amount(saveTransaction.getAmount())
                    .description(saveTransaction.getDescription())
                    .timestamp(saveTransaction.getTimestamp())
                    .build();
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transaction", transactionRestModel);

            return ResponseEntity.ok(RestModel.builder().data(responseData).message("Top Up " + model.getAmount() + "฿ to " + payee.getInfo().getFirst_name() + " " + payee.getInfo().getLast_name() + " successful").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestModel.builder().message("top up wallet failed").build());
        }
    }
}