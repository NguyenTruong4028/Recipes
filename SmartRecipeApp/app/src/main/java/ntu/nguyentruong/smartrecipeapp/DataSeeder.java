package ntu.nguyentruong.smartrecipeapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataSeeder {
    public static List<MonAn> getSampleRecipes(String currentUserId, String currentUserName) {
        List<MonAn> list = new ArrayList<>();

        // Món 1: Phở Bò
        MonAn mon1 = new MonAn();
        mon1.setTenMon("Phở Bò Tái Nạm");
        mon1.setHinhAnh("https://i.ytimg.com/vi/3bN7xKz3Z6k/maxresdefault.jpg"); // Link ảnh mạng
        mon1.setThoiGian("45 phút");
        mon1.setKhauPhan("2 người");
        mon1.setDoKho("Trung bình");
        mon1.setNguyenLieu(Arrays.asList("500g Bánh phở", "300g Thịt bò", "Hành tây, hành lá", "Gia vị phở"));
        mon1.setCachLam(Arrays.asList("Hầm xương bò lấy nước dùng.", "Thái thịt bò mỏng.", "Trần bánh phở.", "Chan nước dùng và thưởng thức."));
        mon1.setAuthorId(currentUserId);    // Gán cho User hiện tại làm tác giả
        mon1.setAuthorName(currentUserName);
        mon1.setStatus("Đã duyệt");         // Đã duyệt để hiện lên Home
        mon1.setLikeCount(120);
        list.add(mon1);

        // Món 2: Trứng chiên
        MonAn mon2 = new MonAn();
        mon2.setTenMon("Trứng Chiên Hành");
        mon2.setHinhAnh("https://cdn.tgdd.vn/2020/07/Cook/TM/cach-lam-trung-chien-nuoc-mam-don-gian-ma-cuc-ngon-com-tai-nha-o-thumb-620x620.jpg");
        mon2.setThoiGian("10 phút");
        mon2.setKhauPhan("1 người");
        mon2.setDoKho("Dễ");
        mon2.setNguyenLieu(Arrays.asList("2 quả trứng", "Hành lá", "Nước mắm", "Dầu ăn"));
        mon2.setCachLam(Arrays.asList("Đập trứng ra bát, đánh tan.", "Thái nhỏ hành lá cho vào.", "Đun nóng dầu, đổ trứng vào chiên vàng 2 mặt."));
        mon2.setAuthorId(currentUserId);
        mon2.setAuthorName(currentUserName);
        mon2.setStatus("Đã duyệt");
        mon2.setLikeCount(15);
        list.add(mon2);

        // Món 3: Salad
        MonAn mon3 = new MonAn();
        mon3.setTenMon("Salad Cá Ngừ");
        mon3.setHinhAnh("https://cdn.tgdd.vn/2021/04/Cook/TM/cach-lam-salad-ca-ngu-giam-can-don-gian-tai-nha-thumb-620x620.jpg");
        mon3.setThoiGian("15 phút");
        mon3.setKhauPhan("2 người");
        mon3.setDoKho("Dễ");
        mon3.setNguyenLieu(Arrays.asList("1 hộp cá ngừ", "Xà lách", "Cà chua bi", "Sốt Mayonnaise"));
        mon3.setCachLam(Arrays.asList("Rửa sạch rau.", "Trộn cá ngừ với rau.", "Rưới sốt lên và trộn đều."));
        mon3.setAuthorId("admin_id_fake"); // Giả vờ người khác đăng
        mon3.setAuthorName("Đầu bếp Master");
        mon3.setStatus("Đã duyệt");
        mon3.setLikeCount(89);
        list.add(mon3);

        return list;
    }
}
