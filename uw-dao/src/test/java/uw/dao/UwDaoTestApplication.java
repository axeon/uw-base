package uw.dao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uw.dao.sequence.DaoSequenceFactory;

@SpringBootApplication
public class UwDaoTestApplication {

    public static void main(String[] args) {
        SpringApplication.run( UwDaoTestApplication.class, args );
        String seqName = "test5";
//        long dbSeqId = DaoSequenceFactory.allocateSequenceRange( seqName, 1000 );
//        System.out.println(dbSeqId);
        System.out.println(DaoSequenceFactory.getSequenceId( seqName ));
        for (int i=0;i<3005;i++) {
            SequenceFactory.getSequenceId( seqName );
//            System.out.println( SequenceFactory.getSequenceId( seqName ) );
//            System.out.println(DaoSequenceFactory.getSequenceId( seqName ));
        }
        System.out.println("ok!");

    }


}
