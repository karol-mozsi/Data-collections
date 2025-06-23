## build reasoned version of SNOMED
java -jar -Xmx8g robot_1.5.0-SNAPSHOT.jar \
reason --reasoner ELK \
--input owl_build/SnomedCT_USEditionRF2_PRODUCTION_20190301T120000Z.owl \
--output owl_build/snomedct_us_reasoned_elk_2019-01-13.owl

## convert reasoned version to turtle
# java -jar -Xmx8g robot_1.5.0-SNAPSHOT.jar \
# convert --input owl_build/snomedct_us_reasoned_elk_2019-01-13.owl \
# --output owl_build/snomedct_us_reasoned_elk_2019-01-13.ttl

## build relaxed version of SNOMED
java -jar -Xmx8g robot_1.5.0-SNAPSHOT.jar \
relax --input owl_build/snomedct_us_reasoned_elk_2019-01-13.owl \
--output owl_build/snomedct_us_reasoned_elk_relaxed_2019-01-13.owl

## convert relaxed version to turtle
# java -jar -Xmx8g robot_1.5.0-SNAPSHOT.jar \
# convert --input owl_build/snomedct_us_reasoned_elk_relaxed_2019-01-13.owl \
# --output owl_build/snomedct_us_reasoned_elk_relaxed_2019-01-13.ttl