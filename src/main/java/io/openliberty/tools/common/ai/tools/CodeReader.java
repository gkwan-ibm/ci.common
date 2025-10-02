package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class CodeReader {
    CodingTools codingTools;

    public CodeReader(CodingTools codingTools) {
        this.codingTools = codingTools;
    }

    @UserMessage("""
        You are a professional Coder.
        Read the file with the given filename.
        Return only the contents of the file.
        """)
    @Agent(value = "Reads a file and returns the contents",
            outputName = "code contents")
    public String readFile(@V("File to read. Example (pom.xml)") String fileName) throws Exception {
        return codingTools.readFile(fileName);
    }
}
