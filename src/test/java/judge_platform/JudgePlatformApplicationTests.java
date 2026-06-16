package judge_platform;

import backend.judge.JudgePlatformApplication; // Import your main application class
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = JudgePlatformApplication.class) // Explicitly declare the config source
class JudgePlatformApplicationTests {

	@Test
	void contextLoads() {
	}

}