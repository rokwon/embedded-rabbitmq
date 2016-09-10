package io.arivera.oss.embedded.rabbitmq.bin.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Plugin implements Comparable<Plugin> {

  static final Pattern LIST_OUTPUT_PATTERN = Pattern.compile("\\s*\\[(.*)]\\s+(\\w+)\\s+(\\S+)\\s*");

  private static final Logger LOGGER = LoggerFactory.getLogger(Plugin.class);

  private String pluginName;
  private EnumSet<State> status;
  private String version;

  private Plugin(String pluginName, EnumSet<State> state, String version) {
    this.pluginName = pluginName;
    this.status = state;
    this.version = version;
  }

  /**
   * @param strings all the lines to parse. Those that can't be parsed won't be part of the resulting list.
   */
  public static List<Plugin> fromStrings(Collection<String> strings) {
    List<Plugin> plugins = new ArrayList<>(strings.size());
    for (String string : strings) {
      Plugin plugin = fromString(string);
      if (plugin != null) {
        plugins.add(plugin);
      }
    }
    return plugins;
  }

  /**
   * @param outputLine as generated by the command line {@code rabbitmq-plugins groupedList}
   *
   * @return null if output can't be parsed.
   */
  public static Plugin fromString(String outputLine) {
    Matcher matcher = LIST_OUTPUT_PATTERN.matcher(outputLine);
    if (!matcher.matches()) {
      return null;
    }
    String state = matcher.group(1);
    String pluginName = matcher.group(2);
    String version = matcher.group(3);

    return new Plugin(pluginName, State.fromString(state), version);
  }

  public String getName() {
    return pluginName;
  }

  public EnumSet<State> getState() {
    return status;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Plugin that = (Plugin) other;
    return Objects.equals(pluginName, that.pluginName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pluginName);
  }

  @Override
  public int compareTo(Plugin other) {
    return this.pluginName.compareTo(other.pluginName);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Plugin{");
    sb.append("name='").append(pluginName).append('\'');
    sb.append(", status=").append(status);
    sb.append(", version='").append(version).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public enum State {
    ENABLED_EXPLICITLY, ENABLED_IMPLICITLY, NOT_ENABLED,
    RUNNING, NOT_RUNNING;

    /**
     * Parses a string as given by the command line output from {@code rabbitmq-plugins list} for the characters in
     * between brackets.
     */
    public static EnumSet<State> fromString(String string) {
      EnumSet<State> pluginStatuses = EnumSet.noneOf(State.class);

      char[] chars = string.toCharArray();

      if (chars.length != 2) {
        LOGGER.warn("Parsing of Plugin State might not be accurate since we expect 2 symbols representing: {}",
            Arrays.asList(State.values()));
      }

      if (chars.length <= 0) {
        return pluginStatuses;
      }

      char enabledCharacter = chars[0];
      switch (enabledCharacter) {
        case ' ':
          pluginStatuses.add(NOT_ENABLED);
          break;
        case 'e':
          pluginStatuses.add(ENABLED_IMPLICITLY);
          break;
        case 'E':
          pluginStatuses.add(ENABLED_EXPLICITLY);
          break;
        default:
          LOGGER.warn("Could not parse symbol '{}' for enabled state in: {}", enabledCharacter, string);
      }

      if (chars.length < 2) {
        return pluginStatuses;
      }

      char runningCharacter = string.charAt(1);
      switch (runningCharacter) {
        case '*':
          pluginStatuses.add(RUNNING);
          break;
        case ' ':
          pluginStatuses.add(NOT_RUNNING);
          break;
        default:
          LOGGER.warn("Could not parse symbol '{}' for run state in: {}", runningCharacter, string);
      }

      return pluginStatuses;
    }

  }
}
