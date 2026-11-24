package com.walmart.cms.coreapi.service;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ConfigurationMergeResolverTest {
 @Test void mergesInPrecedenceOrderAndDeepMergesNestedMaps() {
  var result=new ConfigurationMergeResolver().merge(
   Map.of("db",Map.of("timeout",10,"pool",5),"mode","global"),
   Map.of("db",Map.of("timeout",20),"mode","env"),
   Map.of("mode","app"));
  assertEquals("app",result.get("mode"));
  assertEquals(Map.of("timeout",20,"pool",5),result.get("db"));
 }
}
