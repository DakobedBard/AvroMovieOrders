package org.mddarr.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.catalina.Executor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@CommonsLog
@SpringBootApplication
@EnableBinding(AnalyticsBinding.class)
public class AnalyticsApplication {
	@Component
	public static class PageViewEventSource implements ApplicationRunner{
		private final MessageChannel pageViewsOut;
		private final Log log = LogFactory.getLog(getClass());
		public PageViewEventSource( AnalyticsBinding binding){
			this.pageViewsOut = binding.pageViewsOut();
		}

		@Override
		public void run(ApplicationArguments args) throws Exception{
			List<String> names = Arrays.asList("mfisher", "davi", "schu","acuer","jress");
			List<String> pages = Arrays.asList("blog", "sitemap", "init","colophon","about");

			Runnable runnable =() -> {
				String rPage = pages.get(new Random().nextInt(pages.size()));
				String rName = pages.get(new Random().nextInt(names.size()));

				PageViewEvent pageViewEvent = new PageViewEvent(rName, rPage, Math.random() > .5 ? 10:1000);
				Message<PageViewEvent> message = MessageBuilder
						.withPayload(pageViewEvent)
						.setHeader(KafkaHeaders.MESSAGE_KEY, pageViewEvent.getUserId().getBytes())
						.build();
				try {
					this.pageViewsOut.send(message);
					log.info("Sent " + message.toString());
				}catch (Exception e){
					log.error(e);
				}
			};
			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(runnable, 1,1, TimeUnit.SECONDS);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsApplication.class, args);
	}

}

interface AnalyticsBinding{
	String PAGE_VIEWS_OUT = "pvout";

	@Output(PAGE_VIEWS_OUT)
	MessageChannel pageViewsOut();
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class PageViewEvent{
	private String userId, page;
	private long duration;

}