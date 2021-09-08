# onms-kafka-producer-test

It is a simple program to understand how long it takes to build the `OnmsNode` entity payload via Kafka Producer in OpenNMS.

[NMS-13256](https://issues.opennms.org/browse/NMS-13256) was the main motivator for having this program, as we found that adding the recursive content gathered via the SNMP Hardware Inventory adapter was expensive due to how it was implemented.

The program expects a working OpenNMS `etc` directory located in the same place you expect to run this program. It is included with this repository, but it can be an existing one; for instance, run the program from `/opt/opennms`.

Inside of it, the `opennms-datasources.xml` must point to a valid PostgreSQL database with the populated inventory, preferably including hardware inventory content.

To run the problem, you should pass a list of Node IDs separated by space (i.e., each as individual arguments). The program will use the current code of the Kafka Producer to build the Protobuf payload and measure how long it takes to build it and how big it is for each provided Node ID.
