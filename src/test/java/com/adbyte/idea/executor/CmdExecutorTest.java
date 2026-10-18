package com.adbyte.idea.executor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdExecutorTest {

    @Test
    void macTerminalIsActivatedAndReceivesCommandAsArgument() {
        List<String> processCommand = CmdExecutor.buildMacTerminalCommand(
            "claude --prompt \"say 'hello'\"",
            "/tmp/project's folder"
        );

        assertEquals("osascript", processCommand.get(0));
        assertTrue(processCommand.get(2).contains("activate\n"));
        assertTrue(processCommand.get(2).contains("do script (item 1 of argv)"));
        assertEquals("--", processCommand.get(3));
        assertEquals(
            "cd '/tmp/project'\"'\"'s folder' && claude --prompt \"say 'hello'\"",
            processCommand.get(4)
        );
    }
}
