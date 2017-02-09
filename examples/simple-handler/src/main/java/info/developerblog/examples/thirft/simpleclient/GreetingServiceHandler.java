package info.developerblog.examples.thirft.simpleclient;

import com.epam.alfa.link.publicws.wsdashboard.exception.SendCodeException;
import com.epam.alfa.link.publicws.wsdashboard.exception.SignException;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.Document;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.DocumentType;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.OperationType;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.SendCodeRequest;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.SendCodeResponse;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.SignRequest;
import com.epam.alfa.link.publicws.wsdashboard.model.groupsigning.SignResponse;
import com.epam.alfa.link.publicws.wsdashboard.service.groupsigningservice.GroupSigningService;
import java.util.HashSet;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by aleksandr on 01.09.15.
 */
@RestController
public class GreetingServiceHandler implements GroupSigningService.Iface {

    @RequestMapping("/sendSignCode/example")
    public SendCodeRequest test() {
        Document doc = new Document(332908, DocumentType.PaymentRub, OperationType.SendToBankPaymentRub);
        HashSet<Document> set = new HashSet<Document>();
        set.add(doc);
        return new SendCodeRequest(set, "XAJY9G", "UAAARQ", null, "127.0.0.1");
    }

    @RequestMapping("/sendSignCode/toSelf")
    public SendCodeResponse selfTest() {
        try {
            Document doc = new Document(332908, DocumentType.PaymentRub, OperationType.SendToBankPaymentRub);
            HashSet<Document> set = new HashSet<Document>();
            set.add(doc);
            THttpClient httpClient = new THttpClient("http://localhost:8080/sendSignCode");
            TProtocol protocol = new TBinaryProtocol(httpClient);
            GroupSigningService.Client client = new GroupSigningService.Client(protocol);
            SendCodeResponse result = client.sendSignCode(new SendCodeRequest(set, "XAJY9G", "UAAARQ", null, "127.0.0.1"));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/sendSignCode", method = RequestMethod.POST)
    public SendCodeResponse sendSignCode(@RequestBody SendCodeRequest scr) throws SendCodeException, TException {
        try {
            THttpClient httpClient = new THttpClient("http://vlkws6:9081/CS/LK/WSDashboard/GroupSigningService");
            TProtocol protocol = new TBinaryProtocol(httpClient);
            GroupSigningService.Client client = new GroupSigningService.Client(protocol);
            return client.sendSignCode(scr);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @RequestMapping(value = "/sign", method = RequestMethod.POST)
    public SignResponse sign(SignRequest sr) throws SignException, TException {
        THttpClient httpClient = new THttpClient("http://vlkws6:9081/CS/LK/WSDashboard/GroupSigningService");
        TProtocol protocol = new TBinaryProtocol(httpClient);
        GroupSigningService.Client client = new GroupSigningService.Client(protocol);
        return client.sign(new SignRequest("XAAAFS", "UAB3OH", 0, "salt", "code", "127.0.0.1"));
    }

    @RequestMapping(value = "/sign/example", method = RequestMethod.GET)
    public SignRequest sign() throws Exception {
        return new SignRequest("XAAAFS", "UAB3OH", 0, "salt", "code", "127.0.0.1");
    }
}
