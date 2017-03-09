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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.Properties;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AppSettingsLoaderImpl implements AppSettingsLoader {

  private final File homeDir;
  private final String[] cliArguments;

  public AppSettingsLoaderImpl(String[] cliArguments) {
    this(cliArguments, detectHomeDir());
  }

  AppSettingsLoaderImpl(String[] cliArguments, File homeDir) {
    this.cliArguments = cliArguments;
    this.homeDir = homeDir;
  }

  @Override
  public AppSettings load() {
    Properties p = loadPropertiesFile(homeDir);
    p.putAll(CommandLineParser.parseArguments(cliArguments));
    p.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    p = ConfigurationUtils.interpolateVariables(p, System.getenv());

    // the difference between Properties and Props is that the latter
    // supports decryption of values, so it must be used when values
    // are accessed
    Props props = new Props(p);
    ProcessProperties.completeDefaults(props);

    // check JDBC properties and set path to driver
    new JdbcSettings().checkAndComplete(homeDir, props);
    return new AppSettingsImpl(props);
  }

  private static File detectHomeDir() {
    try {
      File appJar = new File(PropsBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      return appJar.getParentFile().getParentFile();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot detect path of main jar file", e);
    }
  }

  private static Properties loadPropertiesFile(File homeDir) {
    Properties p = new Properties();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    if (propsFile.exists()) {
      try (Reader reader = new InputStreamReader(new FileInputStream(propsFile), UTF_8)) {
        p.load(reader);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot open file " + propsFile, e);
      }
    }
    return p;
  }
}
