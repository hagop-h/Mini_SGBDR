package test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import main.PageId;
import main.DBParams;
import main.DiskManager;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Stack;

public class DiskManagerTest {
    private static File tmpDir;
    private static DiskManager diskManager;

    @BeforeAll
    public static void setup() throws IOException {
        tmpDir = Files.createTempDirectory("DB_").toFile();
        DBParams.DBPath = tmpDir.getAbsolutePath();
        DBParams.DMFileCount = 4;
        DBParams.SGBDPageSize = 4096;
        try {
            diskManager = DiskManager.getInstance();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    @AfterAll
    public static void exit() throws IOException {
        for (File file : tmpDir.listFiles())
            file.delete();
        tmpDir.delete();
    }

    /*********
     * Tests *
     *********/

    @Test
    @Order(1)
    public void testAllocAndDealloc() throws IOException {
        final int MAX = 100;
        final Stack<PageId> stack = new Stack<>();
        for (int i = 0; i < MAX; i++) {
            stack.add(diskManager.AllocPage());
            assertEquals(i + 1, diskManager.GetCurrentCountAllocPages());
        }
        for (int i = 0; i < MAX; i++) {
            diskManager.DeallocPage(stack.pop());
            assertEquals(MAX - i - 1, diskManager.GetCurrentCountAllocPages());
        }
    }

    @Test
    @Order(2)
    public void testReadAndWrite() throws IOException {
        final int MAX = 100;
        Stack<PageId> stack = new Stack<>();
        for (int i = 0; i < MAX; i++) {
            PageId pageId = diskManager.AllocPage();

            ByteBuffer writeBuffer = ByteBuffer.allocate(DBParams.SGBDPageSize);
            writeBuffer.putInt(i);
            writeBuffer.flip();
            diskManager.WritePage(pageId, writeBuffer);

            ByteBuffer readBuffer = ByteBuffer.allocate(DBParams.SGBDPageSize);
            diskManager.ReadPage(pageId, readBuffer);
            int data = readBuffer.getInt();

            assertEquals(i, data);

            stack.add(pageId);
        }
        while (!stack.empty())
            diskManager.DeallocPage(stack.pop());
    }

    @Test
    @Order(3)
    public void testWriteAndReadString() throws IOException {
        String phrase = "We are the best, We are BDDA";
        PageId pageId = diskManager.AllocPage();
        ByteBuffer tamponPhrase = ByteBuffer.allocate(DBParams.SGBDPageSize);
        tamponPhrase.put(phrase.getBytes());
        tamponPhrase.flip();
        diskManager.WritePage(pageId, tamponPhrase);

        ByteBuffer tamponLuPhrase = ByteBuffer.allocate(DBParams.SGBDPageSize);
        diskManager.ReadPage(pageId, tamponLuPhrase);
        String phraseLue = new String(tamponLuPhrase.array()).trim();

        assertEquals(phrase, phraseLue);

        diskManager.DeallocPage(pageId);
    }

}
