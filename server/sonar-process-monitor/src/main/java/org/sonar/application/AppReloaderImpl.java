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
package org.sonar.application;

import java.io.IOException;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsImpl;
import org.sonar.application.config.AppSettingsLoader;

public class AppReloaderImpl implements AppReloader {

  private final AppSettingsLoader settingsLoader;

  public AppReloaderImpl(AppSettingsLoader settingsLoader) {
    this.settingsLoader = settingsLoader;
  }

  @Override
  public void reload(AppSettings settings) throws IOException {
    // reload settings
    AppSettings reloaded = settingsLoader.load();
    AppFileSystem appFileSystem = new AppFileSystem(settings.getProps());
    appFileSystem.verifyProps();
    appFileSystem.reset();
    appFileSystem.ensureUnchangedConfiguration(reloaded.getProps());
    ((AppSettingsImpl) settings).reload(reloaded.getProps());

    AppLogging logging = new AppLogging();
    logging.configure(settings.getProps());
  }
}
