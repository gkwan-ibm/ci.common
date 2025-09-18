package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.openliberty.tools.common.ai.util.Assistant;

public class AgentTools {

    private final Integer AGENT_ID = 2;

    Assistant assistant;
    CodingTools codingTools;
    
    public AgentTools(CodingTools codingTools) {
        this.codingTools = codingTools;
    }

    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }

    @Tool("Enable an openliberty feature")
    public void queryAgent(@P("What you would like to do") String query) throws Exception {
        // Use a different Agent to allow concurrent messaging, we can generalize this to any multifile update by having an agent at the beginning
        // return a list of files that need to update based on the query

        String pomXMLContent = codingTools.readFile("pom.xml");
        String newPomXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old pom.xml\n" + pomXMLContent + "\n" +
            "Update the pom.xml and return ONLY the new pom.xml without any codeblocks (Markdown)"
        ).content();

        
        String serverXMLContent = codingTools.readFile("server.xml");
        String newServerXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old server.xml\n" + serverXMLContent + "\n" +
            "Update the server.xml and return ONLY the new server.xml without any codeblocks (Markdown)"
        ).content();
        System.out.println(assistant.chat(AGENT_ID, "What changes did you make to enable those features").content());
        assistant.evictChatMemory(query);


        System.out.println(newServerXML);
        System.out.println(newPomXML);

    }
}
