package com.example.walletservice.controller;

import com.example.walletservice.data.Transaction;
import com.example.walletservice.data.Wallet;
import com.example.walletservice.data.WalletInfo;
import com.example.walletservice.data.user.User;
import com.example.walletservice.email.Email;
import com.example.walletservice.rest.*;
import com.example.walletservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("api/wallets")
public class WalletController {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JwtService jwtService;
    @Value("${user.service.endpoint}")
    private String userServiceEndpoint;
    @Value("${notification.service.endpoint}")
    private String notificationServiceEndpoint;

    @GetMapping
    public ResponseEntity<ResponseRestModel> serviceGetWallet(@RequestHeader(value = "Authorization", required = true) String authorizationHeader) {
        try {
            String[] token = authorizationHeader.split(" ");
            Claims claims = jwtService.parseToken(token[1]);
            String user_id = claims.getSubject().toString();

            User foundUser = new RestTemplate().getForObject(userServiceEndpoint + user_id, User.class);
            if (foundUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseRestModel.builder().message("user id not exist 🔴").build());
            }

            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", user_id);
            if (foundWallet == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseRestModel.builder().message("this user doesn't have wallet 🔴").build());
            }

            WalletRestModel model = WalletRestModel.builder().wallet(foundWallet).build();
            return ResponseEntity.ok(ResponseRestModel.builder().data(model).message("get wallet successful 🟢").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseRestModel.builder()
                            .message("get wallet failed 🔴")
                            .cause(e.getLocalizedMessage())
                            .build());
        }
    }

    private Transaction transferCoins(Wallet payerWallet,
                                      Wallet payeeWallet,
                                      BigDecimal amount) {
        try {
            WalletInfo payerWalletInfo = payerWallet.getInfo();
            payerWalletInfo.setBalance(payerWalletInfo.getBalance().subtract(amount));

            WalletInfo payeeWalletInfo = payeeWallet.getInfo();
            payeeWalletInfo.setBalance(payeeWalletInfo.getBalance().add(amount));

            Transaction transaction = Transaction
                    .builder()
                    .timestamp(LocalDateTime.now().toString())
                    .payer_wallet_id(payerWallet.get_id())
                    .payee_wallet_id(payeeWallet.get_id())
                    .amount(amount)
                    .build();
            Transaction saveTransaction = (Transaction) rabbitTemplate.convertSendAndReceive(
                    "TransactionDirect",
                    "saveTransaction",
                    transaction);

            List<String> payerTransactions = payerWalletInfo.getTransactions();
            payerTransactions.add(saveTransaction.get_id());
            payerWalletInfo.setTransactions(payerTransactions);

            List<String> payeeTransactions = payeeWalletInfo.getTransactions();
            payeeTransactions.add(saveTransaction.get_id());
            payeeWalletInfo.setTransactions(payeeTransactions);

            payerWallet.setInfo(payerWalletInfo);
            payeeWallet.setInfo(payeeWalletInfo);

            rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", payerWallet);
            rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", payeeWallet);

            return saveTransaction;
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
    }

    @Transactional
    @PostMapping("/payment")
    public ResponseEntity<ResponseRestModel> serviceCreatePayment(
            @RequestHeader(value = "Authorization", required = true) String authorizationHeader,
            @RequestBody CreatePaymentRestModel model) {
        try {
            String[] token = authorizationHeader.split(" ");
            Claims claims = jwtService.parseToken(token[1]);
            String payer_id = claims.getSubject().toString();

            if (model.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseRestModel.builder().message("amount must be greater than 0 🔴").build());
            }

            Wallet payerWallet = (Wallet) rabbitTemplate.convertSendAndReceive(
                    "WalletDirect",
                    "getWalletByUserId",
                    payer_id);
            if (payerWallet == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseRestModel.builder().message("payer doesn't have wallet 🔴").build());
            }

            WalletInfo payerInfo = payerWallet.getInfo();
            BigDecimal payerBalance = payerInfo.getBalance();

            if (model.getAmount().compareTo(payerBalance) > 0) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(ResponseRestModel.builder().message("insufficient funds to complete the transaction 🔴").build());
            }

            Wallet adorableStoreWallet = (Wallet) rabbitTemplate.convertSendAndReceive(
                    "WalletDirect",
                    "getWalletById",
                    "6551620a29186532bb77f53c");

            Transaction transactionFromPayerToAdorableStore = this.transferCoins(payerWallet, adorableStoreWallet, model.getAmount().add(model.getTax()));

            for (OrderRestModel order : model.getOrders()) {
                Wallet payeeWallet = (Wallet) rabbitTemplate.convertSendAndReceive(
                        "WalletDirect",
                        "getWalletById",
                        order.getWallet_id());

                adorableStoreWallet = (Wallet) rabbitTemplate.convertSendAndReceive(
                        "WalletDirect",
                        "getWalletById",
                        "6551620a29186532bb77f53c");
                Transaction transactionFromAdorableStoreToPayee = this.transferCoins(adorableStoreWallet, payeeWallet, order.getPrice());

//                User payer = new RestTemplate().getForObject(userServiceEndpoint + payer_id, User.class);
//                User payee = new RestTemplate().getForObject(userServiceEndpoint + payeeWallet.getUser_id(), User.class);

//                Email paymentEmailForPayer = new Email(payer, transactionFromAdorableStoreToPayee);
//                NotificationModel notificationModelForPayer = NotificationModel
//                    .builder()
//                    .user_id(payerWallet.getUser_id())
//                    .title(paymentEmailForPayer.getPayerEmailTitle())
//                    .content(paymentEmailForPayer.getPayerEmailContent())
//                    .build();
//                String response1 = new RestTemplate().postForObject(
//                    notificationServiceEndpoint,
//                    notificationModelForPayer,
//                    String.class);
//
//                Email paymentEmailForPayee = new Email(payee, transactionFromAdorableStoreToPayee);
//                NotificationModel notificationModelForPayee = NotificationModel
//                    .builder()
//                    .user_id(payeeWallet.getUser_id())
//                    .title(paymentEmailForPayee.getPayeeEmailTitle())
//                    .content(paymentEmailForPayee.getPayeeEmailContent())
//                    .build();
//                String response2 = new RestTemplate().postForObject(
//                    notificationServiceEndpoint,
//                    notificationModelForPayee,
//                    String.class);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transaction", transactionFromPayerToAdorableStore);
            return ResponseEntity.ok(ResponseRestModel.builder().data(responseData).message("payment successful 🟢").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseRestModel.builder()
                            .message("payment failed 🔴")
                            .cause(e.getLocalizedMessage())
                            .build());
        }
    }

    @GetMapping("/transactions/{transaction_id}")
    public ResponseEntity<ResponseRestModel> serviceGetTransactionById(@RequestHeader(value = "Authorization", required = true) String authorizationHeader, @PathVariable("transaction_id") String transaction_id) {
        try {
            String[] token = authorizationHeader.split(" ");
            Claims claims = jwtService.parseToken(token[1]);
            String user_id = claims.getSubject().toString();

            Wallet walletOwner = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", user_id);
            if (walletOwner == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseRestModel.builder().message("this user doesn't have wallet 🔴").build());
            }

            List<String> transactions = walletOwner.getInfo().getTransactions();
            if (transactions.contains(transaction_id)) {
                Transaction transaction = (Transaction) rabbitTemplate.convertSendAndReceive("TransactionDirect", "getTransactionById", transaction_id);

                String payer_id = null;
                if (transaction.getPayer_wallet_id() != null) {
                    payer_id = (String) rabbitTemplate.convertSendAndReceive("WalletDirect", "getUserIdByWalletId", transaction.getPayer_wallet_id());
                }
                String payee_id = (String) rabbitTemplate.convertSendAndReceive("WalletDirect", "getUserIdByWalletId", transaction.getPayee_wallet_id());

                User payer = new RestTemplate().getForObject(userServiceEndpoint + payer_id, User.class);
                User payee = new RestTemplate().getForObject(userServiceEndpoint + payee_id, User.class);

                TransactionRestModel transactionRestModel = TransactionRestModel.builder()
                        ._id(transaction_id)
                        .payer(payer)
                        .payee(payee)
                        .amount(transaction.getAmount())
                        .timestamp(transaction.getTimestamp())
                        .build();
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("transaction", transactionRestModel);

                return ResponseEntity.ok(ResponseRestModel.builder().data(responseData).message("get transaction successful 🟢").build());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseRestModel.builder().message("transaction id not exist 🔴").build());
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseRestModel.builder()
                            .message("get transactions failed 🔴")
                            .cause(e.getLocalizedMessage())
                            .build());
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<ResponseRestModel> serviceActivateWallet(@RequestBody Wallet wallet) {
        try {
            User foundUser = new RestTemplate().getForObject(userServiceEndpoint + wallet.getUser_id(), User.class);
            if (foundUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseRestModel.builder().message("user id not exist 🔴").build());
            }

            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletByUserId", wallet.getUser_id());
            if (foundWallet != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseRestModel.builder().message("this user already activate wallet 🔴").build());
            }

            Wallet createWallet = Wallet.builder().user_id(wallet.getUser_id()).info(new WalletInfo()).build();
            Wallet activatedWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", createWallet);
            WalletRestModel walletRestModel = WalletRestModel.builder().wallet(activatedWallet).build();
            return ResponseEntity.ok(ResponseRestModel.builder().data(walletRestModel).message("activate wallet successful 🟢").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseRestModel.builder()
                            .message("activate wallet failed 🔴")
                            .cause(e.getLocalizedMessage())
                            .build());
        }
    }

    @PostMapping("/topUp")
    public ResponseEntity<ResponseRestModel> serviceTopUp(@RequestBody TopUpRestModel model) {
        try {
            Wallet foundWallet = (Wallet) rabbitTemplate.convertSendAndReceive("WalletDirect", "getWalletById", model.getWallet_id());
            if (foundWallet == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseRestModel.builder().message("wallet id not exist 🔴").build());
            }
            User payee = new RestTemplate().getForObject(userServiceEndpoint + foundWallet.getUser_id(), User.class);

            if (model.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseRestModel.builder().message("amount must be greater than 0 🔴").build());
            }

            WalletInfo walletInfo = foundWallet.getInfo();
            BigDecimal balance = walletInfo.getBalance();
            walletInfo.setBalance(balance.add(model.getAmount()));

            Transaction saveTransaction = Transaction.builder()
                    .payee_wallet_id(model.getWallet_id())
                    .amount(model.getAmount())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            saveTransaction = (Transaction) rabbitTemplate.convertSendAndReceive("TransactionDirect", "saveTransaction", saveTransaction);

            List<String> transactions = walletInfo.getTransactions();
            transactions.add(saveTransaction.get_id());
            walletInfo.setTransactions(transactions);
            foundWallet.setInfo(walletInfo);
            rabbitTemplate.convertSendAndReceive("WalletDirect", "saveWallet", foundWallet);

            Email topUpEmail = new Email(payee, saveTransaction);
            NotificationModel notificationModel = NotificationModel.builder().user_id(foundWallet.getUser_id()).title(topUpEmail.getTopUpEmailTitle()).content(topUpEmail.getTopUpEmailContent()).build();
            String response = new RestTemplate().postForObject("http://localhost:8081/api/notifications", notificationModel, String.class);

            TransactionRestModel transactionRestModel = TransactionRestModel.builder()
                    ._id(saveTransaction.get_id())
                    .payee(payee)
                    .amount(saveTransaction.getAmount())
                    .timestamp(saveTransaction.getTimestamp())
                    .build();
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transaction", transactionRestModel);

            String coinType = (model.getAmount().compareTo(BigDecimal.ONE) > 0) ? "coins" : "coin";
            String message = "Top Up " + formatNumber(model.getAmount()) + " " + coinType + " to " + payee.getInfo().getFirst_name() + " " + payee.getInfo().getLast_name() + " successful";

            return ResponseEntity.ok(ResponseRestModel.builder().data(responseData).message(message).build());
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseRestModel.builder()
                            .message("top up wallet failed")
                            .cause(e.getLocalizedMessage())
                            .build());
        }
    }

    private static String formatNumber(BigDecimal number) {
        // Create a DecimalFormat instance with the pattern "#,###"
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // Format the BigDecimal number with commas
        return decimalFormat.format(number);
    }
}