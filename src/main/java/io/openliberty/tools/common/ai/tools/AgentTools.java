package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.openliberty.tools.common.ai.util.Assistant;

public class AgentTools implements ToolInterface {

    private final Integer AGENT_ID = 2;
    
    private String output = "";

    Assistant assistant;
    CodingTools codingTools;
    OpenLibertyTools openLibertyTools = new OpenLibertyTools();
    
    public AgentTools(CodingTools codingTools) {
        this.codingTools = codingTools;
    }

    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }

    @Tool("Enable an openliberty feature")
    public String enableOpenLibertyFeature(@P("What you would like to do") String query) throws Exception {
        // Use a different Agent to allow concurrent messaging, we can generalize this to any multifile update by having an agent at the beginning
        // return a list of files that need to update based on the query

        assistant.chat(AGENT_ID, "Here are all the installable openliberty features" + openLibertyTools.getInstallableLibertyFeatures(null));

        String pomXMLContent = codingTools.readFile("pom.xml");
        String newPomXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old pom.xml\n" + pomXMLContent + "\n" +
            "Update the pom.xml and return ONLY all the contents from start to beginning of the new pom.xml without any codeblocks (Markdown).\n" +
            "Ensure the new code can run properly as this code will be injected straight into the project and fulfills what the user wants\n"
        ).content();


        String serverXMLContent = codingTools.readFile("server.xml");
        String newServerXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old server.xml: \n" + serverXMLContent + "\n" +
            "Update the server.xml and return ONLY all the contents from start to beginning of the new server.xml without any codeblocks (Markdown).\n" +
            "Ensure the new code can run properly as this code will be injected straight into the project and fulfills what the user wants\n"
        ).content();

        if (!assistant.chat(AGENT_ID, "Will the new server.xml and pom.xml work? reply with only [y | n]").content().equalsIgnoreCase("y")) {
            String explanation = assistant.chat(AGENT_ID, "Why won't it work").content();
            assistant.evictChatMemory(AGENT_ID);
            return explanation;
        }

        String agenticResponse = assistant.chat(AGENT_ID, "What changes did you make to enable those features").content();
        assistant.evictChatMemory(AGENT_ID);

        codingTools.rewriteFile("pom.xml", newPomXML);
        codingTools.rewriteFile("server.xml", newServerXML);
        return agenticResponse;
    }

    public String getOutput() {
        return output;
    }

    public void flushOutput() {
        output = "";
    }
}
