package uk.gov.ons.ctp.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/**
 * This class reads a yml formatted file and replaces field values with the value of a corresponding
 * environment variable.
 *
 * <p>To match the names of environment variables to yml node names the following rules are applied:
 * 1) convert to lower case 2) replace '.', '-' or '_' with '#'
 *
 * <p>This means that the value of the field 'rabbitmq: username' will be replaced by the contents
 * of the environment variable named 'RABBITMQ_USERNAME'. It can also be replaced with the contents
 * of 'RabbitMQ.UserName'.
 */
public class YmlConfigReader {
  private static final Logger log = LoggerFactory.getLogger(YmlConfigReader.class);

  private Map<String, String> envVariables = null;

  private JsonNode updatedProperties;

  /**
   * Constructor which reads the YML file and updates fields with any overrides that are currently
   * set in environment variables.
   *
   * @param resourcePath is the name of the YML that must be on the class path.
   * @throws CTPException if anything goes wrong.
   */
  public YmlConfigReader(String resourcePath) throws CTPException {
    // In preparation for substitution get hold of all environment variables
    this.envVariables = getNormalisedEnvironmentVariables();

    // Use Jackson to read in the content of the yml file
    JsonNode properties = readYmlFile(resourcePath);

    // Replace property values with those from environment variables
    String rootNodeName = "";
    traverseAndUpdateProperties(properties, rootNodeName);
    this.updatedProperties = properties;
  }

  /**
   * This method uses Jackson to convert the current state of the property data into a Java object.
   *
   * @param <T> The result object type.
   * @param clazz is the class to convert the data into.
   * @return Object containing the configuration date.
   * @throws CTPException if the conversion failed.
   */
  public <T> T convertToObject(Class<T> clazz) throws CTPException {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

      return mapper.treeToValue(this.updatedProperties, clazz);

    } catch (JsonProcessingException e) {
      String errorMessage = "Failed to convert JsonNode properties to class";
      log.error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }
  }

  private Map<String, String> getNormalisedEnvironmentVariables() throws CTPException {
    LinkedHashMap<String, String> cleanedEnvVariables = new LinkedHashMap<>();

    Map<String, String> envVariables = System.getenv();
    for (String name : envVariables.keySet()) {
      String value = envVariables.get(name);
      String cleanedName = normaliseFieldName(name);

      if (cleanedEnvVariables.containsKey(cleanedName)) {
        String errorMessage = "Duplicate environment variable found for: '" + name + "'";
        log.error(errorMessage);
        throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
      }

      cleanedEnvVariables.put(cleanedName, value);
    }

    return cleanedEnvVariables;
  }

  private String normaliseFieldName(String name) {
    return name.replaceAll("(\\.|-|_)", "#").toLowerCase();
  }

  // Get Jackson to read YML file
  private JsonNode readYmlFile(String resourcePath) throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Reading yml file from '{}'", resourcePath);
    }

    JsonNode config;

    try (InputStream rabbitConfigStream =
        getClass().getClassLoader().getResource(resourcePath).openStream()) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      config = mapper.readTree(rabbitConfigStream);
    } catch (IOException e) {
      String errorMessage = "Failed to read contents of configuration file: " + resourcePath;
      log.error(errorMessage, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e, errorMessage);
    }

    return config;
  }

  // Recursively search for properties to be updated
  private void traverseAndUpdateProperties(JsonNode node, String pathToNode) throws CTPException {
    Iterator<Entry<String, JsonNode>> iter = node.fields();
    while (iter.hasNext()) {
      Entry<String, JsonNode> entry = (Entry<String, JsonNode>) iter.next();
      String key = entry.getKey();
      JsonNode childNode = entry.getValue();

      if (childNode.isArray()) {
        // Ignore, as updates on properties not supported
      } else if (childNode.isContainerNode()) {
        traverseAndUpdateProperties(childNode, createPathToNode(pathToNode, key));
      } else {
        // Found a property
        applyNodeEnvironmentalOverride(node, pathToNode, key);
      }
    }
    ;
  }

  // Updates the value of a child node with the contents of a matching environment variable.
  // No update is applied if there is no matching environment variable.
  private void applyNodeEnvironmentalOverride(
      JsonNode parentNode, String pathToParent, String nodeName) throws CTPException {
    // Build full name of node, eg 'rabbit#host'
    String rawPathToNode = createPathToNode(pathToParent, nodeName);
    String pathToNode = normaliseFieldName(rawPathToNode);

    if (envVariables.containsKey(pathToNode)) {
      ObjectNode objectNode = (ObjectNode) parentNode;
      if (!objectNode.has(nodeName)) {
        String errorMessage = "Error: Attempting to set value for unknown node: '" + nodeName + "'";
        log.error(errorMessage);
        throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
      }

      // Update value of node
      String replacementValue = envVariables.get(pathToNode);
      JsonNode currentValue = objectNode.get(nodeName);
      if (log.isDebugEnabled()) {
        log.debug(
            "Updating value of node '"
                + pathToNode
                + "' from '"
                + currentValue.asText()
                + "' to '"
                + replacementValue
                + "'");
      }
      objectNode.put(nodeName, replacementValue);
    }
  }

  private String createPathToNode(String parentNodePath, String childNodeName) {
    if (parentNodePath.isEmpty()) {
      return childNodeName;
    } else {
      return parentNodePath + "_" + childNodeName;
    }
  }
}
