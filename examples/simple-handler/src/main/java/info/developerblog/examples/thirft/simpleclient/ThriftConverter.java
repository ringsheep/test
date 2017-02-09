package info.developerblog.examples.thirft.simpleclient;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author stCarolas stcarolas@gmail.com
 */
public class ThriftConverter extends AbstractGenericHttpMessageConverter {

    public static class ThriftMapping {

        private Class inArgsClass;
        private Class outArgsClass;
        private String methodName;

        public Class getInArgsClass() {
            return inArgsClass;
        }

        public void setInArgsClass(Class inArgsClass) {
            this.inArgsClass = inArgsClass;
        }

        public Class getOutArgsClass() {
            return outArgsClass;
        }

        public void setOutArgsClass(Class outArgsClass) {
            this.outArgsClass = outArgsClass;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

    }

    private static final HashMap<Class, ThriftMapping> mapping = new HashMap<>();

    TProtocolFactory factory = new TBinaryProtocol.Factory();

    public ThriftConverter() {
        super(new MediaType("application", "x-thrift"));
    }

    @Override
    protected void writeInternal(Object t, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Class parameterClass = null;
        try {
            parameterClass = Class.forName(type.getTypeName());
        } catch (ClassNotFoundException ex) {
            return;
        }
        ThriftMapping outMapping = mapping.get(parameterClass);
        TIOStreamTransport transport = new TIOStreamTransport(outputMessage.getBody());
        TProtocol protocol = factory.getProtocol(transport);
        TBase result = null;
        try {
            result = (TBase) outMapping.getOutArgsClass().newInstance();
            result.setFieldValue(result.fieldForId(0), t);
            protocol.writeMessageBegin(new TMessage(outMapping.getMethodName(), TMessageType.REPLY, 1));
            result.write(protocol);
            protocol.writeMessageEnd();
//            protocol.writeMessageBegin(message);
//            result = (TBase) outMapping.getOutArgsClass().newInstance();
//            result.read(protocol);
//            protocol.readMessageEnd();
        } catch (Exception ex) {
            Logger.getLogger(ThriftConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected boolean supports(Class clazz) {
        return true;
    }

    @Override
    protected Object readInternal(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object read(Type type, Class contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        Class parameterClass = null;
        try {
            parameterClass = Class.forName(type.getTypeName());
        } catch (ClassNotFoundException ex) {
            return null;
        }

        ThriftMapping inMapping = null;
        if (mapping.containsKey(parameterClass)) {
            inMapping = mapping.get(parameterClass);
        } else {

            Class ifaceClass = contextClass.getInterfaces()[0];
            Method controllerCalledMethod = null;
            for (Method method : ifaceClass.getMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.getType().equals(parameterClass)) {
                        controllerCalledMethod = method;
                    }
                }
            }
            String methodName = controllerCalledMethod.getName();
            Class returnType = controllerCalledMethod.getReturnType();
            String inArgsClassName = "$" + methodName + "_args";
            String outArgsClassName = "$" + methodName + "_result";

            Class<TProcessor> processorClass = null;
            Class<TServiceClient> clientClass = null;
            Class<TBase> argsClass = null;
            Class<TBase> outArgs = null;

            Class serviceClass = ifaceClass.getEnclosingClass();
            for (Class<?> innerClass : serviceClass.getDeclaredClasses()) {
                if (innerClass.getName().endsWith("$Processor")) {
                    if (!TProcessor.class.isAssignableFrom(innerClass)) {
                        continue;
                    }
                    processorClass = (Class<TProcessor>) innerClass;
                }

                if (innerClass.getName().endsWith(inArgsClassName)) {
                    argsClass = (Class<TBase>) innerClass;
                }

                if (innerClass.getName().endsWith(outArgsClassName)) {
                    outArgs = (Class<TBase>) innerClass;
                }

                if (innerClass.getName().endsWith("$Client")) {
                    if (!TServiceClient.class.isAssignableFrom(innerClass)) {
                        continue;
                    }
                    clientClass = (Class<TServiceClient>) innerClass;
                }
            }
            inMapping = new ThriftMapping();
            inMapping.setInArgsClass(argsClass);
            inMapping.setOutArgsClass(outArgs);
            inMapping.setMethodName(methodName);
            mapping.put(parameterClass, inMapping);
            mapping.put(returnType, inMapping);
        }
        TIOStreamTransport transport = new TIOStreamTransport(inputMessage.getBody());
        TProtocol protocol = factory.getProtocol(transport);
        TBase result = null;
        try {
            protocol.readMessageBegin();
            result = (TBase) inMapping.getInArgsClass().newInstance();
            result.read(protocol);
            protocol.readMessageEnd();
        } catch (Exception ex) {
            Logger.getLogger(ThriftConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
//        try {
//            Constructor<TServiceClient> constructor = clientClass.getConstructor(TProtocol.class);
//            TServiceClient client = constructor.newInstance(protocol);
//            Method receiveMethod = TServiceClient.class.getDeclaredMethod("receiveBase", TBase.class, String.class);
//            receiveMethod.setAccessible(true);
//
//            receiveMethod.invoke(client, result, controllerCalledMethod.getName());
//        } catch (Exception ex) {
//            Logger.getLogger(ThriftConverter.class.getName()).log(Level.SEVERE, null, ex);
//        }

        return result.getFieldValue(result.fieldForId(1));
    }

}
