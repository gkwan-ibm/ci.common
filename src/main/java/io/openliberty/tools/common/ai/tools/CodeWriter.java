package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class CodeWriter {
    CodingTools codingTools;

    public CodeWriter(CodingTools codingTools) {
        this.codingTools = codingTools;
    }

    @UserMessage("""
        You are a professional Coder.
        Write the code to the file using the tool.
        """)
    @Agent("Replaces the file to have the contents given")
    public void rewriteFile(@V("File to write to. Example (pom.xml)") String fileName, @V("Content to write") String contents) throws Exception {
        codingTools.rewriteFile(fileName, contents);
    }
}
