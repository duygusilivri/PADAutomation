package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.json.*;


/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        

        try {
            String token = getTokenForTriggeringPAD(context);
            context.getLogger().info("Token: " + token);

            String padflowid = triggerPADwithCallbackURL(context, token);
            
            return request.createResponseBuilder(HttpStatus.OK).body(padflowid).build();


        } catch (Exception e) {
            e.printStackTrace();
            context.getLogger().info("Error: " + e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.OK).body("Hello world!").build();
            
            
    }

    public static String getTokenForTriggeringPAD(ExecutionContext context)
    {
        try {
            //todo: replace entra_tenant_id with your tenant id
            String tenantId = "entra_tenant_id";
            //todo: confirm the URL is correct. Go to Entra -> App Registrations -> Your App -> Endpoints -> OAuth 2.0 token endpoint 
            URL url = new URL("https://login.microsoftonline.com/"+tenantId+"/oauth2/v2.0/token");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
           
            //todo: replace app_client_id with the client id of your app. Go to Entra -> App Registrations -> Your App -> Overview -> Application (client) ID
            String clientId = "app_client_id";
            //todo: replace app_client_secret with the client secret value of your app. Go to Entra -> App Registrations -> Your App -> Manage -> Certificates & secrets -> Client secret value
            String clientSecret = "app_client_secret";
            //todo: replace scope_url with the power platform environment url. Go to Power Platform -> Environments -> Your Environment -> Environment URL
            String scope = "https://"+"scope_url"+"/.default";
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());

            String formURLencodedString = "grant_type=client_credentials&client_id="+clientId+"&client_secret="+clientSecret+"&scope="+encodedScope;
            context.getLogger().info("formURLencodedString: " + formURLencodedString);
    
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = formURLencodedString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                int responseCode = con.getResponseCode();
                context.getLogger().info("Response Code: " + responseCode);

                String responseMsg = con.getResponseMessage();
                context.getLogger().info("Response Msg: " + responseMsg);
                context.getLogger().info("Response Body: " + response.toString());


                JSONObject jsonObject = new JSONObject(response.toString());
                String accessToken =  jsonObject.getString("access_token");
              
           
                return accessToken;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                con.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
    }

    public static String triggerPADwithCallbackURL(ExecutionContext context, String token)
    {
        try {
            //todo: replace workflow_id with the workflow id of the Power Automate Desktop flow. Go to Power Automate -> Your Flow -> find the workflow id in the URL
            String workflowid = "workflow_id";
            //todo: replace api_endpoint with the power platform web api endpoint. Go to Power Apps -> Settings -> Developer Resources -> Web API Endpoint
            URL url = new URL("https://"+"api_endpoint"+"/workflows("+workflowid+")/Microsoft.Dynamics.CRM.RunDesktopFlow");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer "+token);
            con.setRequestProperty("Content-Type", "application/json");
    
            String runMode = "attended";
            String runPriority = "normal";
            String timeout = "7200";
            //todo: replace connection_name with the connection name you created for the service principal with Postman / Bruno
            String connectionName = "connection_name";
            String connectionType = "1";
            //todo: replace callback_url with the callback URL of the Logic App. Go to Logic Apps -> Your Workflow -> Overview -> Workflow URL
            String callbackUrl = "callback_url";
            String jsonInputString = "{\"runMode\": \""+runMode+"\", \"runPriority\": \""+runPriority+"\", \"timeout\": \""+timeout+"\", \"connectionName\": \""+connectionName+"\", \"connectionType\": \""+connectionType+"\" , \"callbackUrl\": \""+callbackUrl+"\"}";
            context.getLogger().info("jsonInputString: " + jsonInputString);

            // Write the JSON data to the output stream
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = con.getResponseCode();
            context.getLogger().info("Response Code: " + responseCode);

            String responseMsg = con.getResponseMessage();
            context.getLogger().info("Response Msg: " + responseMsg);
           
            // Capture and print the response body
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                context.getLogger().info("Response Body: " + response.toString());

                JSONObject jsonObject = new JSONObject(response.toString());
                String padflowid =  jsonObject.getString("flowsessionId");
                context.getLogger().info("padflowid: " + padflowid);
                return padflowid;
            }
       
            catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                con.disconnect();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
    }

    
}
