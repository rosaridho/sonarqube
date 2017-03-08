package org.sonar.application;

import org.junit.Test;
import org.sonar.application.cluster.AppStateClusterImpl;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class AppStateFactoryTest {

  private TestAppSettings settings = new TestAppSettings();
  private AppStateFactory underTest = new AppStateFactory(settings);

  @Test
  public void create_cluster_implementation_if_cluster_is_enabled() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_NAME, "foo");

    assertThat(underTest.create()).isInstanceOf(AppStateClusterImpl.class);
  }

  @Test
  public void cluster_implementation_is_disabled_by_default() {
    assertThat(underTest.create()).isInstanceOf(AppStateImpl.class);

  }
}
