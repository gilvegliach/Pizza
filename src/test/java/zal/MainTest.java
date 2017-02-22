package zal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MainTest {
  @Test
  public void pass() {
    Main mock = mock(Main.class);
    assertEquals(mock, mock);
    assertThat(1, is(1));
  }
}
