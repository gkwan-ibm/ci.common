package io.openliberty.tools.common.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
// import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
// import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import io.openliberty.tools.common.ai.util.Assistant;
import io.openliberty.tools.common.ai.util.ModelBuilder;
import io.openliberty.tools.common.ai.util.Utils;

public class AgentTools implements ToolInterface {

    // private ModelBuilder modelBuilder = new ModelBuilder();
    
    private String output = "";
    private final Integer AGENT_ID = 2;

    Assistant assistant;
    CodingTools codingTools;
    OpenLibertyTools openLibertyTools = new OpenLibertyTools();

    CodeGenerator codeGenerator;
    HumanInTheLoop humanInTheLoop;
    SupervisorAgent codeSupervisor;
    
    public AgentTools(CodingTools codingTools) {
        this.codingTools = codingTools;
        // codeGenerator = AgenticServices
        //                 .agentBuilder(CodeGenerator.class)
        //                 .chatModel(modelBuilder.getChatModel())
        //                 .build();

        // humanInTheLoop = AgenticServices
        //         .humanInTheLoopBuilder()
        //         .description("An agent that asks for clarification")
        //         .outputName("sign")
        //         .requestWriter(request -> {
        //             System.out.println(request);
        //             System.out.print("> ");
        //         })
        //         .responseReader(() -> System.console().readLine())
        //         .build();

        // codeSupervisor = AgenticServices
        //         .supervisorBuilder()
        //         .chatModel(modelBuilder.getChatModel())
        //         .subAgents(codeGenerator, humanInTheLoop, new CodeReader(codingTools), new CodeWriter(codingTools))
        //         .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
        //         .build();
    }

    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }

    @Tool("Enable an openliberty feature")
    public String enableOpenLibertyFeature(@P("What you would like to do") String query) throws Exception {
        // Use a different Agent to allow concurrent messaging, we can generalize this to any multifile update by having an agent at the beginning
        // return a list of files that need to update based on the query
        Utils.confirm("Here we start\n");
        Utils.confirm(query);
        String filter = assistant.chat(AGENT_ID, "What exact feature is the user looking for? Here is the user query" +
                    query + 
                    "include the name only so I can filter and find for you (Example: openAPI)").content();

        Utils.confirm(filter);

        assistant.chat(AGENT_ID, "Here are all the installable openliberty features, if there arent any that the user wants, dont make any changes..." + openLibertyTools.getInstallableLibertyFeatures(filter));
        String pomXMLContent = codingTools.readFile("pom.xml");
        String newPomXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old pom.xml\n" + pomXMLContent + "\n" +
            "Update the pom.xml and return ONLY all the contents from start to beginning of the new pom.xml without any codeblocks (Markdown).\n"
        ).content();


        String serverXMLContent = codingTools.readFile("server.xml");
        String newServerXML = assistant.chat(AGENT_ID,
            "With the following query given by the user: " + query + "\n" +
            "Here is the old server.xml: \n" + serverXMLContent + "\n" +
            "Update the server.xml and return ONLY all the contents from start to beginning of the new server.xml without any codeblocks (Markdown).\n"
        ).content();

        if (!assistant.chat(AGENT_ID, "Will your changes satisfy the user? reply with only one character, no formatting [y | n]").content().equalsIgnoreCase("y")) {
            String explanation = assistant.chat(AGENT_ID, "Why won't it work").content();
            assistant.evictChatMemory(AGENT_ID);
            return explanation;
        }

        String agenticResponse = assistant.chat(AGENT_ID, "What changes did you make to enable those features? Be descriptive and explain why").content();
        assistant.evictChatMemory(AGENT_ID);

        Utils.confirm(newServerXML);
        Utils.confirm(newPomXML);
        return "The agent may have altered the pom.xml and server.xml, here is what it wanted to respond with: " + agenticResponse;
    }

    // @Tool("Enable an openliberty feature")
    // public String enableOpenLibertyFeature(@P("What you would like to do") String query) throws Exception {
    //     return codeSupervisor.invoke("Here are all the installable openliberty features: " + openLibertyTools.getInstallableLibertyFeatures(null)
    //                                     + " Can you enable the openliberty feature by updating the pom.xml and server.xml appropriately?"
    //                                     + " Do not assume the pom.xml and server.xml path, only input 'pom.xml' and 'server.xml' to the tools. "
    //                                     + " Use your agents to read the file, generate the new code, then save it. "
    //                                     + " If you are unable to save the new server.xml/pom.xml return why. "
    //                                     + " Otherwise, return what you did and explain what you did in a detailed and consise manner. "
    //                                     + " Here is the user's query: " + query);
    // }

    public String getOutput() {
        return output;
    }

    public void flushOutput() {
        output = "";
    }
}
