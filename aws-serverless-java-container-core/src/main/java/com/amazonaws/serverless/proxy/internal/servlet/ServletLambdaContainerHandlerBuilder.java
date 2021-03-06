package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Base builder class for {@link AwsLambdaServletContainerHandler}. Implmentations can extend this class to have setters
 * for the basic parameters.
 * @param <RequestType> The event object class
 * @param <ResponseType> The output object class
 * @param <ContainerRequestType> The container request type. For proxy implementations, this is {@link AwsProxyHttpServletRequest}.
 *                              The response type is hardcoded to {@link AwsHttpServletResponse} since it is a generic
 *                              servlet response implementation.
 * @param <HandlerType> The type of the handler we are building
 * @param <Builder> The builder object itself. This is used to allow implementations to re-use the setter method from this
 *                 abstract class through <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=133275">
 *                     "curiously recurring generic patterns"</a>
 */
public abstract class ServletLambdaContainerHandlerBuilder<
        RequestType,
        ResponseType,
        ContainerRequestType extends HttpServletRequest,
        HandlerType extends AwsLambdaServletContainerHandler<RequestType, ResponseType, ContainerRequestType, AwsHttpServletResponse>,
        Builder extends ServletLambdaContainerHandlerBuilder<RequestType, ResponseType, ContainerRequestType, HandlerType, Builder>>
{
    private static final String MISSING_FIELD_ERROR = "Missing %s in lambda container handler builder";

    protected InitializationWrapper initializationWrapper;
    protected RequestReader<RequestType, ContainerRequestType> requestReader;
    protected ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter;
    protected SecurityContextWriter<RequestType> securityContextWriter;
    protected ExceptionHandler<ResponseType> exceptionHandler;
    protected Class<RequestType> requestTypeClass;
    protected Class<ResponseType> responseTypeClass;

    /**
     * Validates that all of the required fields are populated.
     * @throws ContainerInitializationException If values have not been set on the builder. The message in the exception
     *  contains a standard error message {@link ServletLambdaContainerHandlerBuilder#MISSING_FIELD_ERROR} populated with
     *  the list of missing fields.
     */
    protected void validate() throws ContainerInitializationException {
        List<String> errFields = new ArrayList<>();
        if (requestTypeClass == null) {
            errFields.add("request type class");
        }
        if (responseTypeClass == null) {
            errFields.add("response type class");
        }
        if (requestReader == null) {
            errFields.add("request reader");
        }
        if (responseWriter == null) {
            errFields.add("response writer");
        }
        if (securityContextWriter == null) {
            errFields.add("security context writer");
        }
        if (exceptionHandler == null) {
            errFields.add("exception handler");
        }
        if (initializationWrapper == null) {
            errFields.add("initialization wrapper");
        }
        if (!errFields.isEmpty()) {
            throw new ContainerInitializationException(String.format(MISSING_FIELD_ERROR, String.join(", ", errFields)), null);
        }
    }

    /**
     * Sets all of the required fields in the builder to the default settings for a Servlet-compatible framework that wants
     * to support AWS proxy event and output types.
     * @return A populated builder
     */
    public Builder defaultProxy() {
        initializationWrapper(new InitializationWrapper())
                .requestReader((RequestReader<RequestType, ContainerRequestType>) new AwsProxyHttpServletRequestReader())
                .responseWriter((ResponseWriter<AwsHttpServletResponse, ResponseType>) new AwsProxyHttpServletResponseWriter())
                .securityContextWriter((SecurityContextWriter<RequestType>) new AwsProxySecurityContextWriter())
                .exceptionHandler((ExceptionHandler<ResponseType>) new AwsProxyExceptionHandler())
                .requestTypeClass((Class<RequestType>) AwsProxyRequest.class)
                .responseTypeClass((Class<ResponseType>) AwsProxyResponse.class);
        return self();
    }

    /**
     * Sets the initialization wrapper to be used by the {@link ServletLambdaContainerHandlerBuilder#buildAndInitialize()}
     * method to start the framework implementations
     * @param initializationWrapper An implementation of <code>InitializationWrapper</code>. In most cases, this will be
     *                              set to {@link InitializationWrapper}. The {@link ServletLambdaContainerHandlerBuilder#asyncInit(long)}
     *                              method sets this to {@link AsyncInitializationWrapper}.
     * @return This builder object
     */
    public Builder initializationWrapper(InitializationWrapper initializationWrapper) {
        this.initializationWrapper = initializationWrapper;
        return self();
    }

    public Builder requestReader(RequestReader<RequestType, ContainerRequestType> requestReader) {
        this.requestReader = requestReader;
        return self();
    }

    public Builder responseWriter(ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter) {
        this.responseWriter = responseWriter;
        return self();
    }

    public Builder securityContextWriter(SecurityContextWriter<RequestType> securityContextWriter) {
        this.securityContextWriter = securityContextWriter;
        return self();
    }

    public Builder exceptionHandler(ExceptionHandler<ResponseType> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return self();
    }

    public Builder requestTypeClass(Class<RequestType> requestType) {
        this.requestTypeClass = requestType;
        return self();
    }

    public Builder responseTypeClass(Class<ResponseType> responseType) {
        this.responseTypeClass = responseType;
        return self();
    }

    public Builder asyncInit(long actualStartTime) {
        this.initializationWrapper = new AsyncInitializationWrapper(actualStartTime);
        return self();
    }

    /**
     * Implementations should implement this method to return their type. All of the builder methods in this abstract
     * class use this method to return the correct builder type.
     * @return The current builder.
     */
    protected abstract Builder self();
    public abstract HandlerType build() throws ContainerInitializationException;
    public abstract HandlerType buildAndInitialize() throws ContainerInitializationException;
}
