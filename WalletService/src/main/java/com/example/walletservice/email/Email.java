package com.example.walletservice.email;

import com.example.walletservice.data.Transaction;
import com.example.walletservice.data.user.User;
import com.example.walletservice.utils.DateTime;
import lombok.Data;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Data
public class Email {
    private User user;
    private String content;
    private String coinType;
    private Transaction transaction;
    private DateTime dateTime;

    public Email(User user, Transaction transaction) {
        this.user = user;
        this.transaction = transaction;
        this.dateTime = new DateTime();
        this.coinType = (transaction.getAmount().compareTo(BigDecimal.ONE) > 0) ? "coins" : "coin";
    }
    public String getTopUpEmailTitle() {
        return "แจ้งเตือนการเติมเงินสำเร็จ! กระเป๋าเงินของคุณพร้อมใช้งานแล้ว ✨";
    }
    public String getPayerEmailTitle() {
        return "เราขอแจ้งให้ทราบว่าการชำระเงินด้วย Wallet ของคุณที่ Adorable Store เสร็จสมบูรณ์! 👝";
    }
    public String getPayeeEmailTitle() {
        return "เราขอแจ้งให้ทราบว่ามีผู้สั่งซื้อสินค้าของคุณที่ Adorable Store! 😁";
    }
    public String getTopUpEmailContent() {
        return  "<html>"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<center>"
                + "<img src=\"https://cdn.discordapp.com/attachments/933591523189215235/1172994986120315010/logo.png?ex=656257b5&is=654fe2b5&hm=0dd0b6259550b3718479ab27cd652e19f174c0e36888d11891ddac364eb174ac&\" width=\"300\" height=\"36\">"
                + "<div style=\"display:flex; justify-content:\"center\"\">"
                + "<h1>สวัสดี "
                + "<b>" + user.getInfo().getFirst_name() + " " + user.getInfo().getLast_name() + "</b> 👋🏻</h1>"
                + "</div>"
                + "<br>"
                + "<p>ขอบคุณที่ใช้บริการ Adorable Store ตลอดเวลาที่ผ่านมา! เพื่อทำให้การช็อปปิ้งของคุณเป็นประสบการณ์ที่สะดวกสบายมากขึ้น<br>ทางทีมงานขอแจ้งเตือนให้ทราบว่าการเติมเงินลงในกระเป๋าเงิน Adorable Store ของคุณเสร็จสมบูรณ์!</p>"
                + "<br>"
                + "<b>รายละเอียดการเติมเงิน:</b>"
                + "<p>หมายเลข Transaction: <b>" + transaction.get_id() + "</b></p>"
                + "<p>วันที่เติม: " + DateTime.getDateAndTime(transaction.getTimestamp())[0] + "</p>"
                + "<p>เวลาที่เติม: " + DateTime.getDateAndTime(transaction.getTimestamp())[1] + "</p>"
                + "<p>จำนวนเงิน: <b>" + formatNumber(transaction.getAmount()) + " " + this.coinType + "</b></p>"
                + "<br>"
                + "<p>กรุณาตรวจสอบยอดเงินในกระเป๋าเงิน Adorable Store ของคุณเพื่อให้แน่ใจว่าการทำธุรกรรมของคุณเสร็จสมบูรณ์และยอดเงินถูกต้องตามที่คุณต้องการค่ะ</p>"
                + "<p>ขอบคุณที่ให้ความไว้วางใจใน Adorable Store ของเรา เราหวังว่าคุณจะพึงพอใจกับสินค้าและบริการของเราต่อไป</p>"
                + "<br>"
                + "<p>ขอแสดงความนับถือ</p>"
                + "<p>ทีมงาน Adorable Store 🐈</p>"
                + "</center>"
                + "</body>"
                + "</html>";
    }
    public String getPayerEmailContent() {
        return  "<html>"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<center>"
                + "<img src=\"https://cdn.discordapp.com/attachments/933591523189215235/1172994986120315010/logo.png?ex=656257b5&is=654fe2b5&hm=0dd0b6259550b3718479ab27cd652e19f174c0e36888d11891ddac364eb174ac&\" width=\"300\" height=\"36\">"
                + "<div style=\"display:flex; justify-content:\"center\"\">"
                + "<h1>สวัสดี "
                + "<b>" + user.getInfo().getFirst_name() + " " + user.getInfo().getLast_name() + "</b> 👋🏻</h1>"
                + "</div>"
                + "<br>"
                + "<p>ขอบคุณที่ใช้บริการ Adorable Store ตลอดเวลาที่ผ่านมา!</p>"
                + "<br>"
                + "<b>รายละเอียดการทำธุรกรรม:</b>"
                + "<p>หมายเลข Transaction: <b>" + transaction.get_id() + "</b></p>"
                + "<p>วันที่ทำธุรกรรม: " + DateTime.getDateAndTime(transaction.getTimestamp())[0] + "</p>"
                + "<p>เวลาที่ทำธุรกรรม: " + DateTime.getDateAndTime(transaction.getTimestamp())[1] + "</p>"
                + "<p>จำนวนเงิน: <b>" + formatNumber(transaction.getAmount()) + " " + this.coinType + "</b> coins</p>"
                + "<br>"
                + "<p>กรุณาตรวจสอบยอดเงินในกระเป๋าเงิน Adorable Store ของคุณเพื่อให้แน่ใจว่าการทำธุรกรรมของคุณเสร็จสมบูรณ์ตามที่คุณต้องการค่ะ</p>"
                + "<p>ขอบคุณที่ให้ความไว้วางใจใน Adorable Store ของเรา เราหวังว่าคุณจะพึงพอใจกับสินค้าและบริการของเราต่อไป</p>"
                + "<br>"
                + "<p>ขอแสดงความนับถือ</p>"
                + "<p>ทีมงาน Adorable Store 🐈</p>"
                + "</center>"
                + "</body>"
                + "</html>";
    }
    public String getPayeeEmailContent() {
        return  "<html>"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<center>"
                + "<img src=\"https://cdn.discordapp.com/attachments/933591523189215235/1172994986120315010/logo.png?ex=656257b5&is=654fe2b5&hm=0dd0b6259550b3718479ab27cd652e19f174c0e36888d11891ddac364eb174ac&\" width=\"300\" height=\"36\">"
                + "<div style=\"display:flex; justify-content:\"center\"\">"
                + "<h1>สวัสดี "
                + "<b>" + user.getInfo().getFirst_name() + " " + user.getInfo().getLast_name() + "</b> 👋🏻</h1>"
                + "</div>"
                + "<br>"
                + "<p>ขอบคุณที่ใช้บริการ Adorable Store ตลอดเวลาที่ผ่านมา!</p>"
                + "<br>"
                + "<b>รายละเอียดการทำธุรกรรม:</b>"
                + "<p>หมายเลข Transaction: <b>" + transaction.get_id() + "</b></p>"
                + "<p>วันที่สั่งซื้อสินค้า: " + DateTime.getDateAndTime(transaction.getTimestamp())[0] + "</p>"
                + "<p>เวลาที่สั่งซื้อสินค้า: " + DateTime.getDateAndTime(transaction.getTimestamp())[1] + "</p>"
                + "<p>จำนวนเงิน: <b>" + formatNumber(transaction.getAmount()) + " " + this.coinType + "</b> coins</p>"
                + "<br>"
                + "<p>กรุณาตรวจสอบยอดเงินในกระเป๋าเงิน Adorable Store ของคุณเพื่อให้แน่ใจว่าการทำธุรกรรมเสร็จสมบูรณ์ค่ะ</p>"
                + "<p>ขอบคุณที่ให้ความไว้วางใจใน Adorable Store ของเรา เราหวังว่าคุณจะพึงพอใจกับสินค้าและบริการของเราต่อไป</p>"
                + "<br>"
                + "<p>ขอแสดงความนับถือ</p>"
                + "<p>ทีมงาน Adorable Store 🐈</p>"
                + "</center>"
                + "</body>"
                + "</html>";
    }
    private static String formatNumber(BigDecimal number) {
        // Create a DecimalFormat instance with the pattern "#,###"
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // Format the BigDecimal number with commas
        return decimalFormat.format(number);
    }
}
