package org.opennms.tests;

import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.spring.BeanUtils;
import org.opennms.features.kafka.producer.ProtobufMapper;
import org.opennms.features.kafka.producer.model.OpennmsModelProtos;
import org.opennms.netmgt.config.api.EventConfDao;
import org.opennms.netmgt.dao.api.HwEntityDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.SessionUtils;
import org.opennms.netmgt.model.OnmsNode;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CriteriaCheck {
    public static void main(String[] args) {
        System.out.println("This program assumes the existence of the etc directory pointing to a running DB to analyze.");
        if (args == null || args.length == 0) {
            System.out.println("Please provide a list of Node IDs from the database.");
            System.exit(1);
        }
        System.setProperty("opennms.home", System.getProperty("user.dir"));
        System.setProperty("activemq.data", "temp/activemq");

        EventConfDao eventConfDao = BeanUtils.getBean("commonContext", "eventConfDao", EventConfDao.class);
        HwEntityDao entityDao = BeanUtils.getBean("daoContext", "hwEntityDao", HwEntityDao.class);
        SessionUtils sessionUtils = BeanUtils.getBean("daoContext", "sessionUtils", SessionUtils.class);
        NodeDao nodeDao = BeanUtils.getBean("daoContext", "nodeDao", NodeDao.class);
        TransactionTemplate transactionTemplate = BeanUtils.getBean("daoContext", "transactionTemplate", TransactionTemplate.class);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                ProtobufMapper mapper = new ProtobufMapper(eventConfDao, entityDao, sessionUtils, nodeDao, 20000);
                CriteriaBuilder builder = new CriteriaBuilder(OnmsNode.class);
                builder.in("id", Arrays.asList(args).stream().map(Integer::parseInt).collect(Collectors.toList()));
                List<OnmsNode> nodes = nodeDao.findMatching(builder.toCriteria());
                System.out.println("Found " + nodes.size() + " nodes.");
                long total_before = System.currentTimeMillis();
                for (OnmsNode node : nodes) {
                    System.out.println("Processing node " + node.getLabel() + "(" + node.getNodeId() + ")");
                    try {
                        long before = System.currentTimeMillis();
                        OpennmsModelProtos.Node n = mapper.toNode(node).build();
                        long after = System.currentTimeMillis();
                        System.out.println("  It took " + (after-before) + "ms to create the protobuf object");
                        System.out.println("  The protobuf payload size is " + n.toByteArray().length + " bytes");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                long total_after = System.currentTimeMillis();
                System.out.println("Finished in " + (total_after-total_before) + "ms");
            }
        });
    }
}
