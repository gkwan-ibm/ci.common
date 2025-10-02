package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CodeGenerator {
    @UserMessage("""
        You are a professional Coder.
        Analyze and enhance the following old code based on the prompt of {{prompt}}.
        Return only the full contents of new code and nothing else.
        The new code should be able to run
        The old code is "{{oldCode}}".
        """)
    @Agent(value ="Generates new code based on the prompt",
            outputName =  "New Code")
    String generateNewCode(@V("old code") String oldCode, @V("prompt") String prompt);
}
