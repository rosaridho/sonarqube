/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.sonar.process.ProcessProperties.CLUSTER_ENABLED;

public class ClusterSettings implements Consumer<Props> {

  @Override
  public void accept(Props props) {
    // TODO verify that at least one process is enabled
  }

  public static boolean isClusterEnabled(AppSettings settings) {
    return settings.getProps().valueAsBoolean(CLUSTER_ENABLED);
  }

  public static List<ProcessId> getEnabledProcesses(AppSettings settings) {
    if (!isClusterEnabled(settings)) {
      return Arrays.asList(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
    }
    List<ProcessId> enabled = new ArrayList<>();
    if (!settings.getProps().valueAsBoolean(ProcessProperties.CLUSTER_SEARCH_DISABLED)) {
      enabled.add(ProcessId.ELASTICSEARCH);
    }
    if (!settings.getProps().valueAsBoolean(ProcessProperties.CLUSTER_WEB_DISABLED)) {
      enabled.add(ProcessId.WEB_SERVER);
    }

    if (!settings.getProps().valueAsBoolean(ProcessProperties.CLUSTER_CE_DISABLED)) {
      enabled.add(ProcessId.COMPUTE_ENGINE);
    }
    return enabled;
  }
}
