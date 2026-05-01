package auction.model;

import auction.exception.InvalidBidException;
import auction.model.item.Item;
import auction.model.item.ItemFactory;
import auction.model.users.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 * Test xem ReentrantLock trong AuctionManager.attemptBid()
 * có bảo vệ đúng khi nhiều người đặt giá ĐỒNG THỜI không.
 * ============================================================
 */
public class BidRaceConditionTest {

    private Item          testItem; // Món đồ đang đấu giá
    private AuctionManager manager;

    // Số luồng chạy đồng thời (= số người đặt giá cùng lúc)
    private static final int THREAD_COUNT = 20;

    @BeforeEach
    void setUp() {
        manager  = AuctionManager.getInstance();

        // Tạo item giá khởi điểm 1000, ownerId = "U99"
        testItem = ItemFactory.factoryItem("ELECTRONICS", "U99", "RACE-01", "Race Item", 1000.0);

        // postItem() → OPEN, sẵn sàng đấu giá
        Member poster = new Member("U00", "poster", "pass");
        poster.postItem(testItem);
    }

    // ============================================================
    //  TEST 1 — Giá cuối phải là giá CAO NHẤT trong tất cả các bid
    // ============================================================

    /**
     * Kịch bản: 20 người đặt giá khác nhau CÙNG LÚC (1100, 1200, ..., 3000).
     *
     * Nếu lock hoạt động đúng:
     *   → Giá cuối = giá cao nhất trong số các bid THÀNH CÔNG
     *   → Không bị mất update, không bị ghi đè lung tung
     */
    @Test
    @DisplayName("20 bid đồng thời → giá cuối là giá cao nhất thành công")
    void testConcurrent_FinalPriceIsHighest() throws InterruptedException {
        ExecutorService executor   = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch  startGun   = new CountDownLatch(1);  // Tín hiệu bắt đầu cùng lúc
        CountDownLatch  finishLine = new CountDownLatch(THREAD_COUNT);

        // Lưu các giá bid thành công để so sánh
        List<Double> successAmounts = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final String bidder = "U-" + String.format("%02d", i); // U-00, U-01, ...
            final double amount = 1100.0 + (i * 100.0);           // 1100, 1200, ..., 3000

            executor.submit(() -> {
                try {
                    startGun.await(); // Chờ tín hiệu — đảm bảo tất cả bắt đầu cùng lúc
                    manager.attemptBid(testItem, bidder, amount);
                    successAmounts.add(amount);
                } catch (InvalidBidException e) {
                    // Bình thường — những người đến sau bị "Giá phải lớn hơn"
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();                       // BẮT ĐẦU tất cả cùng lúc
        finishLine.await(10, TimeUnit.SECONDS);     // Chờ tất cả xong (timeout 10s)
        executor.shutdown();

        System.out.println("[Test1] Giá cuối: " + testItem.getCurrentPrice());
        System.out.println("[Test1] Số bid thành công: " + successAmounts.size() + "/" + THREAD_COUNT);

        // Giá cuối phải cao hơn giá khởi điểm
        assertTrue(testItem.getCurrentPrice() > 1000.0,
                "Phải có ít nhất 1 bid thành công → giá phải tăng");

        // Giá cuối phải là giá cao nhất trong các bid đã thành công
        if (!successAmounts.isEmpty()) {
            double maxSuccess = successAmounts.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            assertEquals(maxSuccess, testItem.getCurrentPrice(),
                    "Giá cuối phải bằng bid cao nhất đã thành công");
        }
    }

    // ============================================================
    //  TEST 2 — Tất cả bid CÙNG GIÁ: chỉ đúng 1 người thắng
    // ============================================================

    /**
     * Đây là kịch bản nguy hiểm nhất — mô phỏng race condition thuần túy:
     * 20 người cùng thấy giá 1000, cùng gửi bid 2000 đến Server.
     *
     * Nếu KHÔNG có lock:
     *   → Nhiều người cùng pass `if (amount < currentPrice)` → nhiều người thắng
     * Nếu CÓ lock đúng:
     *   → Chỉ người ĐẾN TRƯỚC (acquire lock trước) được chấp nhận
     *   → Những người sau: giá 2000 == 2000 (không CÒN nhỏ hơn) → exception
     *
     * Kỳ vọng: đúng 1 người thành công
     */
    @Test
    @DisplayName(" 20 người bid cùng giá 2000 → chỉ đúng 1 người thắng")
    void testConcurrent_SamePrice_OnlyOneWins() throws InterruptedException {
        ExecutorService executor   = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch  startGun   = new CountDownLatch(1);
        CountDownLatch  finishLine = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0); // Đếm thread-safe

        final double SAME_BID = 2000.0; // Tất cả đặt cùng mức giá này

        for (int i = 0; i < THREAD_COUNT; i++) {
            final String bidder = "U-" + String.format("%02d", i);

            executor.submit(() -> {
                try {
                    startGun.await();
                    manager.attemptBid(testItem, bidder, SAME_BID);
                    successCount.incrementAndGet(); // Chỉ người đến trước mới vào được đây
                } catch (InvalidBidException e) {
                    // Những người sau sẽ bị "Giá đặt phải lớn hơn giá hiện tại!"
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        finishLine.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("[Test2] Số người thắng (cùng giá 2000): " + successCount.get());

        // Chỉ đúng 1 người được chấp nhận
        assertEquals(1, successCount.get(),
                "Khi 20 người bid cùng giá, chỉ đúng 1 người được chấp nhận");

        // Giá phải bằng đúng mức đặt
        assertEquals(SAME_BID, testItem.getCurrentPrice());
    }

    // ============================================================
    //  TEST 3 — Không có lỗi bất ngờ khi tải nặng (50 luồng)
    // ============================================================

    /**
     * Kịch bản: 50 người đặt giá đồng thời, giá tăng dần nhẹ.
     *
     * Kỳ vọng: chỉ được phép xảy ra InvalidBidException.
     * Không được có NullPointerException, IllegalMonitorStateException,
     * hay bất kỳ lỗi hệ thống nào khác → chứng tỏ lock ổn định.
     */
    @Test
    @DisplayName(" 50 bid đồng thời → không xảy ra lỗi hệ thống bất ngờ")
    void testConcurrent_HeavyLoad_NoUnexpectedErrors() throws InterruptedException {
        final int HEAVY = 50;

        ExecutorService executor   = Executors.newFixedThreadPool(HEAVY);
        CountDownLatch  startGun   = new CountDownLatch(1);
        CountDownLatch  finishLine = new CountDownLatch(HEAVY);

        // Lưu các exception KHÔNG phải InvalidBidException
        List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < HEAVY; i++) {
            final String bidder = "U-" + i;
            final double amount = 1001.0 + (i * 10.0); // Tăng nhẹ để đa dạng kết quả

            executor.submit(() -> {
                try {
                    startGun.await();
                    manager.attemptBid(testItem, bidder, amount);
                } catch (InvalidBidException e) {
                    // Hợp lệ — bỏ qua
                } catch (Exception e) {
                    // Lỗi bất ngờ → ghi lại để báo cáo
                    unexpectedErrors.add(e);
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        finishLine.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // In ra lỗi bất ngờ nếu có — giúp debug dễ hơn
        unexpectedErrors.forEach(e ->
                System.err.println("[Test3] Lỗi bất ngờ: "
                        + e.getClass().getSimpleName() + " — " + e.getMessage())
        );

        assertTrue(unexpectedErrors.isEmpty(),
                "Không được có lỗi hệ thống. Số lỗi bất ngờ: " + unexpectedErrors.size());
    }
}