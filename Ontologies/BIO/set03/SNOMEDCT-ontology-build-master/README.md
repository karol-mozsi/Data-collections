# SNOMEDCT-ontology-build
Repos used for building OWL files from SNOMED CT RF2 files.

Requires use of the OBO robot tool: http://robot.obolibrary.org/

and SNOMED OWL toolkit: https://github.com/IHTSDO/snomed-owl-toolkit

The SNOMED OWL toolkit requires you to download snomed-owl-toolkit-2.2.0-SNAPSHOT-executable.jar, which is found here: https://github.com/IHTSDO/snomed-owl-toolkit/releases

The commands used to convert the RF2 files to owl are in snomedct_rf2_to_owl_intl.sh and snomedct_rf2_to_owl_us.sh.

The commands used to build the reasoned, relaxed, turtle versions of the ontology are in snomedct_intl_reasoned_elk.sh and snomedct_us_reasoned_elk.sh.

**Note:** These shell scripts take a while to run.

Here is an outline of the overall build process:
1. Use the SNOMED OWL toolkit to build the SNOMED OWL file from the RF2 release, e.g.:
```
java -jar snomed-owl-toolkit-2.2.0-executable.jar -rf2-to-owl \ 
-rf2-snapshot-archives SnomedCT_InternationalRF2_PRODUCTION_20190131T120000Z.zip
```
2. Use the robot tool build a reasoned version of SNOMED ontology:
```
java -jar -Xmx8g ~/repos/robot/bin/robot.jar reason \
--reasoner ELK \
--input owl_build/SnomedCT_InternationalRF2_PRODUCTION_20190131T120000Z.owl
--output owl_build/snomedct_intl_reasoned_elk_2019-01-13.owl
```
3. Use OBO robot toot to "relax" (see http://robot.obolibrary.org/relax) the ontology:
```
java -jar -Xmx8g ~/repos/robot/bin/robot.jar \
relax --input owl_build/snomedct_intl_reasoned_elk_2019-01-13.owl \
--output owl_build/snomedct_intl_reasoned_elk_relaxed_2019-01-13.owl
```

Sometimes it is useful to convert an ontology into turtle syntax. Using robot, this can be accomplished using the 'conver' option:
```
java -jar -Xmx8g ~/repos/robot/bin/robot.jar \
convert --input owl_build/snomedct_intl_reasoned_elk_2019-01-13.owl \
--output owl_build/snomedct_intl_reasoned_elk_2019-01-13.ttl
```