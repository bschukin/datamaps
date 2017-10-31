package com.datamaps.util;

import io.methvin.watcher.DirectoryWatcher;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by Щукин on 28.10.2017.
 *
 * DirectoryWatcher - утилита для онлайн-отслеживания изменения маппингов
 */
public class RecursiWatcherServiceTests
{

    @Test(invocationCount = 1)
    public void testRecursiWatcherServiceTests () throws IOException, InterruptedException {
        DirectoryWatcher watcher = DirectoryWatcher.create(new File("D://newfolder").toPath(),
                event -> {
            switch (event.eventType()) {
                case CREATE: /* file created */;
                    System.out.println("create " + event.path());
                    break;
                case MODIFY: /* file modified */;
                    System.out.println("modifi " + event.path());
                    break;
                case DELETE: /* file deleted */;
                    System.out.println("delete " + event.path());
                    break;
            }
        });

        //todo: расскмоентить чтобы смотреть как работает вотчиннг
        // watcher.watch();
    }
}
