package auction.model;

import auction.exception.InvalidBidException;
import auction.model.factory.FactoryProvider;
import auction.model.item.Item;
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
 * Kiểm tra ReentrantLock trong AuctionManager.attemptBid()
 * bảo vệ đúng khi nhiều luồng đặt giá đồng thời.

 *   CountDownLatch startGun  — đảm bảo tất cả luồng xuất phát cùng lúc
 *   CountDownLatch finishLine — chờ tất cả luồng hoàn thành trước khi assert
 *   AtomicInteger             — đếm bid thành công an toàn giữa nhiều luồng
 */
public class BidRaceConditionTest {

    private Item testItem;
    private AuctionManager manager;
    private static final int THREAD_COUNT = 20;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();

        // nanoTime() → itemId duy nhất mỗi test, tránh state leak giữa các test
        // (Singleton AuctionManager dùng chung, itemId giống nhau sẽ bị ảnh hưởng bởi test trước)
        String uniqueId = "RACE-" + System.nanoTime();

        // Dùng constructor 5-param (id, username, password, fullName, email) cho rõ ràng
        Member seller = new Member("U99", "seller_test", "pass", "Seller", "seller@test.com");
        testItem = FactoryProvider.createItemByType("ELECTRONICS", uniqueId, "U99", "Race Item", 1000.0);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        testItem.setStartTime(now.minusMinutes(5));
        testItem.setEndTime(now.plusHours(2));

        seller.postItem(testItem); // PENDING → OPEN
    }

    @Test
    @DisplayName("20 bid đồng thời giá khác nhau → giá cuối là bid cao nhất thành công")
    void testConcurrent_FinalPriceIsHighest() throws InterruptedException {
        CountDownLatch startGun   = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);
        List<Double> successAmounts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final String bidder = "U-" + String.format("%02d", i);
                final double amount = 1100.0 + (i * 100.0); // 1100, 1200, ..., 3100

                executor.submit(() -> {
                    try {
                        startGun.await(); // Chờ tín hiệu bắt đầu cùng lúc
                        manager.attemptBid(testItem, bidder, amount);
                        successAmounts.add(amount);
                    } catch (InvalidBidException e) {
                        // Bình thường — người đến sau bị từ chối
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLine.countDown();
                    }
                });
            }
            startGun.countDown();
            assertTrue(finishLine.await(10, TimeUnit.SECONDS), "Timeout");
        } finally {
            executor.shutdown();
        }

        assertTrue(testItem.getCurrentPrice() > 1000.0, "Phải có ít nhất 1 bid thành công");

        if (!successAmounts.isEmpty()) {
            double maxSuccess = successAmounts.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            // Nếu ReentrantLock đúng: giá cuối == bid thành công cao nhất
            assertEquals(maxSuccess, testItem.getCurrentPrice(),
                    "Giá cuối phải bằng bid cao nhất đã thành công");
        }
    }

    @Test
    @DisplayName("20 người bid cùng giá 2000 → chỉ đúng 1 người thắng")
    void testConcurrent_SamePrice_OnlyOneWins() throws InterruptedException {
        CountDownLatch startGun   = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);
        // AtomicInteger để đếm an toàn giữa các luồng song song
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
                        successCount.incrementAndGet(); // Chỉ luồng vào lock đầu tiên đến được đây
                    } catch (InvalidBidException e) {
                        // Những người sau: "Giá đặt phải lớn hơn giá hiện tại!" → bình thường
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLine.countDown();
                    }
                });
            }
            startGun.countDown();
            assertTrue(finishLine.await(10, TimeUnit.SECONDS), "Timeout");
        } finally {
            executor.shutdown();
        }

        assertEquals(1, successCount.get(), "Cùng giá → chỉ 1 người được chấp nhận");
        assertEquals(sameBid, testItem.getCurrentPrice());
    }

    @Test
    @DisplayName("50 bid đồng thời tải nặng → không có lỗi hệ thống bất ngờ")
    void testConcurrent_HeavyLoad_NoUnexpectedErrors() throws InterruptedException {
        final int heavy = 50;
        CountDownLatch startGun   = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(heavy);
        // Chỉ ghi lại Exception thật sự (không phải InvalidBidException — đó là hợp lệ)
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
                        // Hợp lệ — bỏ qua
                    } catch (Exception e) {
                        // NullPointerException, ClassCastException, ... → lỗi thật sự
                        unexpectedErrors.add(e);
                    } finally {
                        finishLine.countDown();
                    }
                });
            }
            startGun.countDown();
            assertTrue(finishLine.await(15, TimeUnit.SECONDS), "Timeout");
        } finally {
            executor.shutdown();
        }

        assertTrue(unexpectedErrors.isEmpty(),
                "Không được có lỗi hệ thống. Số lỗi bất ngờ: " + unexpectedErrors.size());
    }
}