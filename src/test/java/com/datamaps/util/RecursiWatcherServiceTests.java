package com.datamaps.util;

import io.methvin.watcher.DirectoryWatcher;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Щукин on 28.10.2017.
 *
 * DirectoryWatcher - утилита для онлайн-отслеживания изменения маппингов
 */
public class RecursiWatcherServiceTests
{

    @Test(invocationCount = 1)
    public void testRecursiWatcherServiceTests () throws IOException, InterruptedException {
        ClassPathResource rs = new ClassPathResource("com/datamaps/maps");
        ClassPathResource rs2 = new ClassPathResource("com/datamaps/services");

        List<Path> list = Arrays.asList(rs.getFile().toPath(), rs2.getFile().toPath());

        DirectoryWatcher watcher = DirectoryWatcher.create(list,
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
