package com.github.liejuntao001.jenkins

@Grab('org.yaml:snakeyaml:1.25')

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/*
  Original reference:
  https://github.com/OndraZizka/yaml-merge/blob/master/src/main/java/org/cobbzilla/util/yml/YmlMerger.java
 */

class MyYaml {
  private final Yaml parser

  MyYaml() {
    parser = new Yaml(new SafeConstructor())
  }

  String merge(List<String> yamls) {
    Map<String, Object> mergedResult = new LinkedHashMap<String, Object>();
    for (yaml in yamls) {
      final Map<String, Object> yamlToMerge = parser.load(yaml)
      // Merge into results map.
      mergeStructures(mergedResult, yamlToMerge)
    }
    return parser.dump(mergedResult)
  }

  private static Object addToMergedResult(Map<String, Object> mergedResult, String key, Object yamlValue) {
    return mergedResult.put(key, yamlValue)
  }

  private static IllegalArgumentException unknownValueType(String key, Object yamlValue) {
    final String msg = "Cannot mergeYamlFiles element of unknown type: " + key + ": " + yamlValue.getClass().getName()
    return new IllegalArgumentException(msg)
  }

  private void mergeLists(Map<String, Object> mergedResult, String key, Object yamlValue) {
    if (!(yamlValue instanceof List && mergedResult.get(key) instanceof List)) {
      throw new IllegalArgumentException("Cannot mergeYamlFiles a list with a non-list: " + key)
    }

    List<Object> originalList = (List<Object>) mergedResult.get(key)

    // original implementation
    // originalList.addAll((List<Object>) yamlValue)

    // my implementation
    // below is non-standard approach as I assume a key:value mapping called (name->value) to identify a Map
    List<Object> yamlList = (List<Object>) yamlValue
    Map<String, Object> originalCache = new LinkedHashMap<>()
    String name
    for (ori in originalList) {
      if (ori instanceof Map) {
        name = ori.get('name')
        if (name) {
          originalCache.put(name, ori)
        }
      }
    }

    def merged
    for (item in yamlList) {
      merged = false
      if (item instanceof Map) {
        name = item.get('name')
        if (name && originalCache.containsKey(name)) {
          mergeStructures((Map<String, Object>) originalCache.get(name), (Map<String, Object>) item)
          merged = true
        }
      }
      if (!merged) {
        originalList.add(item)
      }
    }
  }

  private void mergeStructures(Map<String, Object> targetTree, Map<String, Object> sourceTree) {
    if (sourceTree == null) return

    for (String key : sourceTree.keySet()) {

      Object yamlValue = sourceTree.get(key)
      if (yamlValue == null) {
        addToMergedResult(targetTree, key, yamlValue)
        continue
      }

      Object existingValue = targetTree.get(key);
      if (existingValue != null) {
        if (yamlValue instanceof Map) {
          if (existingValue instanceof Map) {
            mergeStructures((Map<String, Object>) existingValue, (Map<String, Object>) yamlValue);
          } else if (existingValue instanceof String) {
            throw new IllegalArgumentException("Cannot mergeYamlFiles complex element into a simple element: " + key)
          } else {
            throw unknownValueType(key, yamlValue)
          }
        } else if (yamlValue instanceof List) {
          mergeLists(targetTree, key, yamlValue)

        } else if (yamlValue instanceof String
            || yamlValue instanceof Boolean
            || yamlValue instanceof Double
            || yamlValue instanceof Integer) {

          addToMergedResult(targetTree, key, yamlValue)

        } else {
          throw unknownValueType(key, yamlValue)
        }

      } else {
        if (yamlValue instanceof Map
            || yamlValue instanceof List
            || yamlValue instanceof String
            || yamlValue instanceof Boolean
            || yamlValue instanceof Integer
            || yamlValue instanceof Double) {

          addToMergedResult(targetTree, key, yamlValue)
        } else {
          throw unknownValueType(key, yamlValue)
        }
      }
    }
  }
}
