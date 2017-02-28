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
package org.sonar.server.component.ws;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.Level;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.component.ws.FilterParser.Criterion;
import org.sonar.server.component.ws.FilterParser.Operator;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.SUBVIEW;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.server.component.ws.FilterParser.Operator.EQ;
import static org.sonar.server.component.ws.FilterParser.Operator.IN;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_QUALIFIER;

class ProjectMeasuresQueryFactory {

  public static final String IS_FAVORITE_CRITERION = "isfavorite";
  public static final String QUERY_KEY = "query";
  private static final Set<String> AVAILABLE_QUALIFIERS = newHashSet(PROJECT, VIEW, SUBVIEW);

  private ProjectMeasuresQueryFactory() {
    // prevent instantiation
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(List<Criterion> criteria, @Nullable Set<String> projectUuids) {
    ProjectMeasuresQuery query = new ProjectMeasuresQuery();
    Optional.ofNullable(projectUuids).ifPresent(query::setProjectUuids);
    criteria.forEach(criterion -> processCriterion(criterion, query));
    return query;
  }

  private static void processCriterion(Criterion criterion, ProjectMeasuresQuery query) {
    String key = criterion.getKey().toLowerCase(ENGLISH);
    switch (key) {
      case IS_FAVORITE_CRITERION:
        break;
      case FILTER_LANGUAGE:
        processInCriterion(criterion, FILTER_LANGUAGE, query::setLanguages);
        break;
      case QUERY_KEY:
        processQuery(criterion, query);
        break;
      case ALERT_STATUS_KEY:
        processQualityGateStatus(criterion, query);
        break;
      case FILTER_QUALIFIER:
        processQualifier(criterion, query);
        break;
      default:
        query.addMetricCriterion(new MetricCriterion(key, getNonNullOperator(criterion), parseValue(getNonNullValue(criterion))));
    }
  }

  private static void processQuery(Criterion criterion, ProjectMeasuresQuery query) {
    Operator operatorValue = getNonNullOperator(criterion);
    String value = criterion.getValue();
    checkArgument(value != null, "Query is invalid");
    checkArgument(EQ.equals(operatorValue), "Query should only be used with equals operator");
    query.setQueryText(value);
  }

  private static void processQualityGateStatus(Criterion criterion, ProjectMeasuresQuery query) {
    String value = getNonNullValue(criterion);
    processOnlyEqualsCriterion(criterion, ALERT_STATUS_KEY, qualityGateStatus -> {
      Level matchingQualityGateStatus = Arrays.stream(Level.values())
        .filter(level -> level.name().equalsIgnoreCase(value)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Unknown quality gate status : '%s'", value)));
      query.setQualityGateStatus(matchingQualityGateStatus);
    });
  }

  private static void processQualifier(Criterion criterion, ProjectMeasuresQuery query) {
    processInCriterion(criterion, FILTER_QUALIFIER, qualifiers -> {
      Set<String> newQualifiers = qualifiers.stream()
        .map(qualifier -> qualifier.toUpperCase(ENGLISH))
        .filter(AVAILABLE_QUALIFIERS::contains)
        .collect(Collectors.toSet());
      checkArgument(newQualifiers.size() == qualifiers.size(), "Only %s values are available for qualifier criteria", AVAILABLE_QUALIFIERS);
      query.setQualifiers(newQualifiers);
    });
  }

  private static void processOnlyEqualsCriterion(Criterion criterion, String key, Consumer<String> consumer) {
    Operator operator = getNonNullOperator(criterion);
    String value = getNonNullValue(criterion);
    checkArgument(EQ.equals(operator), "Only equals operator is available for %s criteria", key);
    consumer.accept(value);
  }

  private static void processInCriterion(Criterion criterion, String key, Consumer<Set<String>> consumer) {
    Operator operator = getNonNullOperator(criterion);
    String value = criterion.getValue();
    List<String> values = criterion.getValues();
    if (value != null && EQ.equals(operator)) {
      consumer.accept(singleton(value));
      return;
    }
    if (!values.isEmpty() && IN.equals(operator)) {
      consumer.accept(new HashSet<>(values));
      return;
    }
    throw new IllegalArgumentException(format("%s should be set either by using '%s = value' or '%s IN (v1, v2)'", key, key, key));
  }

  private static double parseValue(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(format("Value '%s' is not a number", value));
    }
  }

  private static String getNonNullValue(Criterion criterion) {
    return checkNonNull(criterion.getValue(), "Value", criterion.getKey());
  }

  private static Operator getNonNullOperator(Criterion criterion) {
    return checkNonNull(criterion.getOperator(), "Operator", criterion.getKey());
  }

  private static <OBJECT> OBJECT checkNonNull(@Nullable OBJECT value, String label, String key) {
    checkArgument(value != null, "%s cannot be null for '%s'", label, key);
    return value;
  }

}
