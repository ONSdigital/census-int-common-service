package uk.gov.ons.ctp.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.File;
import java.io.IOException;
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
public class OverrideableYmlConfigReader {
  private static final Logger log = LoggerFactory.getLogger(OverrideableYmlConfigReader.class);

  Map<String, String> envVariables = null;

  private JsonNode updatedProperties;

  public OverrideableYmlConfigReader(String resourcePath) throws CTPException {
    // In preparation for substitution get hold of all environment variables
    this.envVariables = getCleanedEnvironmentVariables();

    // Use Jackson to read in the content of the yml file
    JsonNode properties = readYmlFile(resourcePath);

    // Replace property values with those from environment variables
    String rootNodeName = "";
    traverseAndUpdateProperties(properties, rootNodeName);
    this.updatedProperties = properties;
  }

  private Map<String, String> getCleanedEnvironmentVariables() throws CTPException {
    LinkedHashMap<String, String> cleanedEnvVariables = new LinkedHashMap<>();

    Map<String, String> envVariables = System.getenv();
    for (String name : envVariables.keySet()) {
      String value = envVariables.get(name);
      String cleanedName = cleanEnvironmentVariableName(name);

      if (cleanedEnvVariables.containsKey(cleanedName)) {
        String errorMessage = "Duplicate environment variable found for: '" + name + "'";
        log.error(errorMessage);
        throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
      }

      cleanedEnvVariables.put(cleanedName, value);
    }

    return cleanedEnvVariables;
  }

  private String cleanEnvironmentVariableName(String name) {
    return name.replaceAll("(\\.|-|_)", "#").toLowerCase();
  }

  // Get Jackson to read YML file
  private JsonNode readYmlFile(String resourcePath) throws CTPException {
    JsonNode config;
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      File resourceFile = new File(getClass().getClassLoader().getResource(resourcePath).getFile());
      config = mapper.readTree(resourceFile);
    } catch (IOException e) {
      String errorMessage = "Failed to read contents of configuration file: " + resourcePath;
      log.error(errorMessage);
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
        attemptNodeUpdate(node, pathToNode, key);
      }
    }
    ;
  }

  private void attemptNodeUpdate(JsonNode parentNode, String pathToParent, String nodeName)
      throws CTPException {
    // Build full name of node, eg 'rabbit#host'
    String rawPathToNode = createPathToNode(pathToParent, nodeName);
    String pathToNode = cleanEnvironmentVariableName(rawPathToNode);

    if (envVariables.containsKey(pathToNode)) {
      ObjectNode objectNode = (ObjectNode) parentNode;
      if (!objectNode.has(nodeName)) {
        String errorMessage = "Error: Attempting to set value for unknown node: '" + nodeName + "'";
        log.error(errorMessage);
        throw new CTPException(Fault.SYSTEM_ERROR, errorMessage);
      }

      // Update value of node
      String replacementValue = envVariables.get(pathToNode);
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

  /**
   * This method uses Jackson to convert the property data into a Java object.
   *
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
      throw new CTPException(Fault.SYSTEM_ERROR);
    }
  }
}
