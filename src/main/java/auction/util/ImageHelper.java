package auction.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ImageHelper {
    // Khai báo hằng số
    public static final int MAX_IMAGES = 5;
    private static final String IMAGE_DIR = "src/main/resources/images/";

    // Quét và lấy danh sách ảnh của 1 sản phẩm
    public static List<File> getImagesOfItem(String itemId) {
        List<File> imageFiles = new ArrayList<>();
        String[] extensions = {".jpg", ".png", ".jpeg"};
        // Quét rộng tới 20 để phòng trường hợp file bị nhảy cóc số thứ tự
        for (int i = 0; i < 20; i++) {
            for (String ext : extensions) {
                File imgFile = new File(IMAGE_DIR + itemId + "_" + i + ext);
                if (imgFile.exists()) {
                    imageFiles.add(imgFile);
                    break; // Tìm thấy 1 ảnh chuẩn thì không cần thử đuôi khác cho số i này nữa
                }
            }
        }
        return imageFiles;
    }

    public static Image loadImageSafe(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new Image(fis);
        } catch (IOException e) {
            System.err.println("Lỗi: Không thể nạp ảnh " + file.getName());
            return null;
        }
    }

    // re-index để dễ dàng tìm ảnh hơn (do khi sửa đổi hình ảnh sẽ lệch index)
    public static void reindexImages(String itemId) {
        List<File> currentFiles = getImagesOfItem(itemId);

        for (int i = 0; i < currentFiles.size(); i++) {
            File oldFile = currentFiles.get(i);

            // Tách lấy phần đuôi mở rộng (.jpg, .png...)
            String fileName = oldFile.getName();
            String ext = fileName.substring(fileName.lastIndexOf("."));

            // Chế tạo tên chuẩn mới theo đúng số thứ tự i
            File newFile = new File(IMAGE_DIR + itemId + "_" + i + ext);

            // Nếu tên file đang bị lệch pha thì tiến hành đổi tên
            if (!oldFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
                if (oldFile.renameTo(newFile)) {
                    System.out.println("[ImageHelper] Đã tự động dồn tên: " + oldFile.getName() + " -> " + newFile.getName());
                }
            }
        }
    }
            //Thêm ảnh mới
            public static boolean addImage (String itemId, File sourceFile){
                // Kiểm tra xem đã chạm nóc 5 ảnh chưa
                List<File> currentFiles = getImagesOfItem(itemId);
                if (currentFiles.size() >= MAX_IMAGES) {
                    System.out.println("[ImageHelper] Từ chối thêm! Đã đạt giới hạn " + MAX_IMAGES + " ảnh.");
                    return false; // Trả về false báo hiệu thêm thất bại
                }

                try {
                    String fileName = sourceFile.getName();
                    String ext = fileName.substring(fileName.lastIndexOf("."));

                    // Lưu tạm file với số to để chắc chắn không đè lên file cũ
                    File destFile = new File(IMAGE_DIR + itemId + "_15" + ext);
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Gọi chiêu dồn số. File _999 sẽ tự động bị giật lùi về đúng vị trí (VD: _3, _4)
                    reindexImages(itemId);

                    return true; // Báo hiệu thêm thành công

                } catch (IOException e) {
                    System.err.println("[ImageHelper] Lỗi khi copy ảnh mới vào hệ thống!");
                    e.printStackTrace();
                    return false;
                }
            }
        }