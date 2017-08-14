package uk.gov.ons.ctp.common.distributed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;

/**
 * Tests for the DistributedLockManagerRedissonImpl
 *
 * NOTE that this is a UNIT test and NOT an INTEGRATION test - it is not the purpose to test redisson, but to test our
 * wrapper over it.
 * Granted, this makes the assumption that our assumptions about redisson functionality are ... as we assume.
 * But testing any further of redisson itself would be extremely difficult and something we trust redisson has done.
 */
@RunWith(MockitoJUnitRunner.class)
public class DistributedLockManagerRedissonImplTest {

  @Mock
  private RedissonClient redissonClient;


  /**
   * Before the test
   * @throws Exception oops
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  /**
   * Test
   * @throws Exception oops
   */
  @Test
  public void testIsLockedOK() throws Exception {
    RLock mockLock = Mockito.mock(RLock.class);
    Mockito.when(redissonClient.getFairLock(any(String.class))).thenReturn(mockLock);
    Mockito.when(mockLock.tryLock()).thenReturn(true);
    Mockito.when(mockLock.expire(any(Long.class), any(TimeUnit.class))).thenReturn(true);

    DistributedLockManagerRedissonImpl impl = new DistributedLockManagerRedissonImpl("test-root", redissonClient, 10);
    Assert.assertTrue(impl.lock("test-lock"));
  }

  /**
   * Test
   * @throws Exception oops
   */
  @Test
  public void testIsLockedNOTOK() throws Exception {
    RLock mockLock = Mockito.mock(RLock.class);
    Mockito.when(redissonClient.getFairLock(any(String.class))).thenReturn(mockLock);
    Mockito.when(mockLock.tryLock()).thenReturn(false);
    
    DistributedLockManagerRedissonImpl impl = new DistributedLockManagerRedissonImpl("test-root", redissonClient, 10);
    Assert.assertFalse(impl.lock("test-lock"));
  }

  /**
   * Test
   * @throws Exception oops
   */
  @Test
  public void testLockOK() throws Exception {
    RLock mockLock = Mockito.mock(RLock.class);
    Mockito.when(redissonClient.getFairLock(any(String.class))).thenReturn(mockLock);
    Mockito.when(mockLock.tryLock()).thenReturn(true);
    Mockito.when(mockLock.expire(any(Long.class), any(TimeUnit.class))).thenReturn(true);
    
    DistributedLockManagerRedissonImpl impl = new DistributedLockManagerRedissonImpl("root", redissonClient, 10);
    Assert.assertTrue(impl.lock("fred"));
  }

  /**
   * Test
   * @throws Exception oops
   */
  @Test
  public void testLockNOTOK() throws Exception {
    RLock mockLock = Mockito.mock(RLock.class);
    Mockito.when(redissonClient.getFairLock(any(String.class))).thenReturn(mockLock);
    Mockito.when(mockLock.tryLock()).thenReturn(false);
    
    DistributedLockManagerRedissonImpl impl = new DistributedLockManagerRedissonImpl("root", redissonClient, 10);
    Assert.assertFalse(impl.lock("fred"));
  }
}
