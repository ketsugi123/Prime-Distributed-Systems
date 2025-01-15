package grpcPrimeServer.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import grpcPrimeServer.model.ServerDetails;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DockerUtils {
    private static final String primeContainerName = "pct";
    private static final String primeImageName = "cdg16/isprime";
    private static final String redisImageName = "redis";
    private static final String redisContainerName = "redis";
    public static DockerClient getDockerClient() {
        return DockerClientBuilder
                .getInstance()
                .withDockerHttpClient(
                        new ApacheDockerHttpClient.Builder()
                                .dockerHost(URI.create("unix:///var/run/docker.sock")).build()
                )
                .build();
    }

     public static boolean launchPrimeContainer(
             DockerClient dockerClient,
             ServerDetails redisServerDetails,
             Long number
     ) {
        try {
            String primeResult = calculateIsPrime(dockerClient, redisServerDetails, number);
            Boolean result = ResponseUtils.containerResponseToBool(primeResult);
            if (result == null) {
                throw new IllegalArgumentException("Container returned unknown response");
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void launchRedis(DockerClient dockerClient, ServerDetails redisInfo) {

        int redisPort = redisInfo.getServerPort();
        // Set up Docker container for Redis
        String containerName = redisContainerName + redisPort;
        HostConfig hostConfig = HostConfig
                .newHostConfig()
                .withPortBindings(PortBinding.parse(redisPort + ":6379"));

        removeIfExists(dockerClient, containerName);
        launchContainer(dockerClient, containerName, redisImageName, hostConfig);
    }


    public static String calculateIsPrime(DockerClient dockerClient,
                                          ServerDetails redisServerDetails,
                                          Long number
    ) throws InterruptedException {
        String currentIterationName = primeContainerName + UUID.randomUUID();
        List<String> commands = Arrays.asList(
                number.toString(),
                redisServerDetails.getServerAddress(),
                redisServerDetails.getServerPort().toString()
        );

        removeIfExists(dockerClient, currentIterationName);

        CreateContainerResponse containerResponse = dockerClient
                .createContainerCmd(primeImageName)
                .withName(currentIterationName)
                .withCmd(commands)
                .exec();

        StringBuilder builder = new StringBuilder();
        
        
        // launch container and attach logger to fetch it when it completes
        dockerClient.startContainerCmd(containerResponse.getId()).exec();
        dockerClient.logContainerCmd(containerResponse.getId())
                .withStdOut(true)
                .withStdErr(false)
                .withFollowStream(true)
                .withTailAll()
                .exec(new Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        builder.append(frame.toString().replace("STDOUT: ", " ").trim());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        super.onError(throwable);
                    }
                }).awaitCompletion();
        dockerClient.removeContainerCmd(currentIterationName)
                .withContainerId(containerResponse.getId())
                .exec();
        return builder.toString();
    }

    static void launchContainer(
            DockerClient client,
            String containerName,
            String imageName,
            HostConfig hostConfig
    ) {
        CreateContainerResponse containerResponse = client
                .createContainerCmd(imageName)
                .withName(containerName)
                .withHostConfig(hostConfig)
                .exec();

        client.startContainerCmd(containerResponse.getId()).exec();
    }

    private static void removeIfExists(DockerClient client, String containerName) {
        Optional<Container> container = getContainer(containerName, client);
        if (container.isPresent()) {
            Container isPrimeContainer = container.get();
            client.stopContainerCmd(isPrimeContainer.getId()).exec();
            client.removeContainerCmd(isPrimeContainer.getId()).exec();
        }
    }

    public static Optional<Container> getContainer(String containerName, DockerClient client) {
        List<Container> containers = client.listContainersCmd().exec();
        for (Container container : containers) {
            List<String> containerNames = Arrays.asList(container.getNames());
            if (containerNames.contains("/" + containerName)) {
                return Optional.of(container);
            }
        }
        return Optional.empty();
    }

}
