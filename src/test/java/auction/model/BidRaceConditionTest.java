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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test ReentrantLock trong AuctionManager.attemptBid()
 * có bảo vệ đúng khi nhiều người đặt giá đồng thời không.
 */
public class BidRaceConditionTest {

    private Item testItem;
    private AuctionManager manager;

    private static final int THREAD_COUNT = 20;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
        testItem = ItemFactory.factoryItem("ELECTRONICS", "U99", "RACE-01", "Race Item", 1000.0);

        Member poster = new Member("U00", "poster", "pass");
        poster.postItem(testItem);
    }

    /**
     * Kịch bản: 20 người đặt giá khác nhau cùng lúc (1100, 1200, ..., 3000).
     * Kỳ vọng: giá cuối = giá cao nhất trong số các bid thành công.
     */
    @Test
    @DisplayName("20 bid đồng thời - giá cuối là giá cao nhất thành công")
    void testConcurrent_FinalPriceIsHighest() throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);
        List<Double> successAmounts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // try-with-resources đảm bảo executor luôn được đóng
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final String bidder = "U-" + String.format("%02d", i);
                final double amount = 1100.0 + (i * 100.0);

                executor.submit(() -> {
                    try {
                        startGun.await(); // Chờ tín hiệu bắt đầu cùng lúc
                        manager.attemptBid(testItem, bidder, amount);
                        successAmounts.add(amount); // Ghi nhận bid thành công
                    } catch (InvalidBidException e) {
                        // Bình thường - những người đến sau bị từ chối
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLine.countDown();
                    }
                });
            }

            startGun.countDown();
            boolean finished = finishLine.await(10, TimeUnit.SECONDS);
            assertTrue(finished, "Timeout - không phải tất cả luồng kịp chạy xong");
        } finally {
            executor.shutdown();
        }

        assertTrue(testItem.getCurrentPrice() > 1000.0,
                "Phải có ít nhất 1 bid thành công - giá phải tăng");

        if (!successAmounts.isEmpty()) {
            double maxSuccess = successAmounts.stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0);
            assertEquals(maxSuccess, testItem.getCurrentPrice(),
                    "Giá cuối phải bằng bid cao nhất đã thành công");
        }
    }

    /**
     * Kịch bản: 20 người cùng gửi bid 2000 đến Server đồng thời.
     * Kỳ vọng: chỉ đúng 1 người được chấp nhận (người đến trước).
     * Lưu ý: cần sửa AuctionManager dùng amount <= getCurrentPrice()
     * để test này chạy đúng.
     */
    @Test
    @DisplayName("20 người bid cùng giá 2000 - chỉ đúng 1 người thắng")
    void testConcurrent_SamePrice_OnlyOneWins() throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        final double sameBid = 2000.0;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final String bidder = "U-" + String.format("%02d", i);

                executor.submit(() -> {
                    try {
                        startGun.await();
                        manager.attemptBid(testItem, bidder, sameBid);
                        successCount.incrementAndGet(); // Chỉ người đến trước vào được đây
                    } catch (InvalidBidException e) {
                        // Những người sau bị "Giá đặt phải lớn hơn giá hiện tại!"
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLine.countDown();
                    }
                });
            }

            startGun.countDown();
            boolean finished = finishLine.await(10, TimeUnit.SECONDS);
            assertTrue(finished, "Timeout - không phải tất cả luồng kịp chạy xong");
        } finally {
            executor.shutdown();
        }

        assertEquals(1, successCount.get(),
                "Khi 20 người bid cùng giá, chỉ đúng 1 người được chấp nhận");
        assertEquals(sameBid, testItem.getCurrentPrice());
    }

    /**
     * Kịch bản: 50 người đặt giá đồng thời.
     * Kỳ vọng: chỉ được phép xảy ra InvalidBidException,
     * không được có NullPointerException hay lỗi hệ thống khác.
     */
    @Test
    @DisplayName("50 bid đồng thời - không xảy ra lỗi hệ thống bất ngờ")
    void testConcurrent_HeavyLoad_NoUnexpectedErrors() throws InterruptedException {
        final int heavy = 50;
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(heavy);
        List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(heavy);

        try {
            for (int i = 0; i < heavy; i++) {
                final String bidder = "U-" + i;
                final double amount = 1001.0 + (i * 10.0);

                executor.submit(() -> {
                    try {
                        startGun.await();
                        manager.attemptBid(testItem, bidder, amount);
                    } catch (InvalidBidException e) {
                        // Hợp lệ - bỏ qua
                    } catch (Exception e) {
                        // Lỗi bất ngờ → ghi lại
                        unexpectedErrors.add(e);
                    } finally {
                        finishLine.countDown();
                    }
                });
            }

            startGun.countDown();
            boolean finished = finishLine.await(15, TimeUnit.SECONDS);
            assertTrue(finished, "Timeout - không phải tất cả luồng kịp chạy xong");
        } finally {
            executor.shutdown();
        }

        assertTrue(unexpectedErrors.isEmpty(),
                "Không được có lỗi hệ thống. Số lỗi bất ngờ: " + unexpectedErrors.size());
    }
}