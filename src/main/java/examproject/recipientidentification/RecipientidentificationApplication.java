package examproject.recipientidentification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class RecipientidentificationApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RecipientidentificationApplication.class, args);
		DataSource dataSource = context.getBean(DataSource.class);

		try (Connection connection = dataSource.getConnection()) {
			System.out.println("Database connection test: SUCCESS");
			// You can log additional details about the connection if necessary
		} catch (Exception e) {
			System.out.println("Database connection test: FAILURE");
			e.printStackTrace();
		}
	}

}
