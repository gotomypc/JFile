package org.agilewiki.jfile.transactions;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jfile.ForcedWriteRootJid;
import org.agilewiki.jfile.JFile;
import org.agilewiki.jfile.JFileFactories;
import org.agilewiki.jfile.block.Block;
import org.agilewiki.jfile.block.LTBlock;
import org.agilewiki.jfile.transactions.db.StatelessDB;
import org.agilewiki.jfile.transactions.transactionProcessor.ProcessBlock;
import org.agilewiki.jfile.transactions.transactionProcessor.TransactionProcessor;
import org.agilewiki.jid.collection.IGet;
import org.agilewiki.jid.collection.vlenc.IAdd;
import org.agilewiki.jid.scalar.vlens.actor.GetActor;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;
import org.agilewiki.jid.scalar.vlens.actor.SetActor;

import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TransactionListJidTest extends TestCase {
    public void test()
            throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(1);
        Mailbox mailbox = mailboxFactory.createMailbox();
        JAFactory factory = new JAFactory(mailbox);
        (new JFileFactories(mailbox)).setParent(factory);
        factory.defineActorType("helloWorldTransaction", HelloWorldTransaction.class);
        JAFuture future = new JAFuture();
        StatelessDB db = new StatelessDB(mailbox);
        db.setParent(factory);
        TransactionProcessor transactionProcessor = new TransactionProcessor(mailbox);
        transactionProcessor.setParent(db);

        JFile jFile = new JFile(mailbox);
        jFile.setParent(factory);
        Path path = FileSystems.getDefault().getPath("TransactionListJidTest.jf");
        System.out.println(path.toAbsolutePath());
        jFile.fileChannel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);

        RootJid rj = new RootJid(mailbox);
        rj.setParent(db);
        (new SetActor(JFileFactories.TRANSACTION_LIST_JID_TYPE)).send(future, rj);
        TransactionListJid transactionListJid = (TransactionListJid) GetActor.req.send(future, rj);

        (new IAdd(-1)).send(future, transactionListJid);
        TransactionActorJid transactionActorJid = (TransactionActorJid) (new IGet(-1)).send(future, transactionListJid);
        (new SetActor("helloWorldTransaction")).send(future, transactionActorJid);
        (new IAdd(-1)).send(future, transactionListJid);
        transactionActorJid = (TransactionActorJid) (new IGet(-1)).send(future, transactionListJid);
        (new SetActor("helloWorldTransaction")).send(future, transactionActorJid);

        Block block = new LTBlock();
        block.setRootJid(rj);
        long timestamp = System.currentTimeMillis();
        block.setTimestamp(timestamp);
        (new ForcedWriteRootJid(block)).send(future, jFile);

        (new ProcessBlock(block)).send(future, transactionProcessor);

        jFile.fileChannel.close();
        mailboxFactory.close();
    }
}
