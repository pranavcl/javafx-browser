// Browser in JavaFX
// Made by Pranav Rakesh Mishra (reg. no. 245891308)

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import java.lang.IllegalArgumentException;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;

public class Main extends Application {
	// Constants
	final int WINDOW_WIDTH=960;
	final int WINDOW_HEIGHT=720;

	// Sizes of headings in pixels
	final float H1_SIZE = 32;
	final float H2_SIZE = 24;
	final float H3_SIZE = 18.72f;
	final float H4_SIZE = 16;
	final float H5_SIZE = 13.28f;
	final float H6_SIZE = 10.72f;

	final float P_SIZE = 16;

	// Global base URL
	String baseUrl = "";

	@Override
	public void start(Stage primaryStage) {
		// Create the URL bar
		TextField textField = new TextField();
		textField.setPromptText("Enter URL...");
		HBox.setHgrow(textField, Priority.ALWAYS);

		// Create Go button
		Button button = new Button("Go");

		HBox row = new HBox(10, textField, button); // 10px spacing

		VBox container = new VBox(5); // 5px spacing
		container.setMaxWidth(Integer.MAX_VALUE);
		setHomePage(container);

		ScrollPane scrollPane = new ScrollPane(container);
		scrollPane.setFitToWidth(true); // makes content span full width

		textField.setOnAction(e -> makeSearch(textField.getText(), container));
		button.setOnAction(e -> makeSearch(textField.getText(), container));

		VBox root = new VBox(10, row, scrollPane); // 10px
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
		root.setPadding(new Insets(10));

		// Create scene (window)
		Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
		primaryStage.setTitle("Pranav's Browser");
		primaryStage.setScene(scene);
		primaryStage.centerOnScreen();
		primaryStage.show();
	}

	void renderTag(String tagName, String text, String tag, VBox root) {
	    Label label = new Label(text);

	    if (tagName.equals("h1")) {
		label.setStyle("-fx-font-size: " + H1_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("h2")) {
		label.setStyle("-fx-font-size: " + H2_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("h3")) {
		label.setStyle("-fx-font-size: " + H3_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("h4")) {
		label.setStyle("-fx-font-size: " + H4_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("h5")) {
		label.setStyle("-fx-font-size: " + H5_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("h6")) {
		label.setStyle("-fx-font-size: " + H6_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("p") || tagName.equals("div") || tagName.equals("span") || tagName.equals("bdi")) {
		label.setStyle("-fx-font-size: " + P_SIZE + "px;");
	    } else if (tagName.equals("strong") || tagName.equals("b")) {
		label.setStyle("-fx-font-size: " + P_SIZE + "px; -fx-font-weight: bold;");
	    } else if (tagName.equals("i") || tagName.equals("em")) {
		label.setStyle("-fx-font-size: " + P_SIZE + "px; -fx-font-style: italic;");
	    } else if (tagName.equals("small")) {
		label.setStyle("-fx-font-size: " + H6_SIZE + "px;");
	    } else if (tagName.equals("li")) {
		label.setStyle("-fx-font-size: " + P_SIZE + "px;");
		label.setText("• " + text);
	    } else if (tagName.equals("a")) {
		label.setStyle("-fx-font-size: " + P_SIZE + "px; -fx-text-fill: blue;");

		String href = "";
		int hrefIdx = tag.indexOf("href=\"");
		if (hrefIdx != -1) {
			int start = hrefIdx + 6; // skip past href="
			int end = tag.indexOf("\"", start);
			if (end != -1) href = tag.substring(start, end);
		}
		
		final String url = href; // needs to be final for lambda
		label.setCursor(javafx.scene.Cursor.HAND);
		label.setOnMouseClicked(e -> makeSearch(url, root));

	    } else if (tagName.equals("br") || tagName.equals("hr")) {
		label.setText(""); // empty line
	    } else {
		// unimplemented tag
		label.setStyle("-fx-font-size: " + P_SIZE + "px;");
		label.setText(tagName + ": " + text);
	    }

	    root.getChildren().add(label);
	}

	void parse(String content, VBox container) {
		container.getChildren().clear();

		Stack<String> stack = new Stack<>();
		String[] nonClosing = {"br", "img", "hr", "input"};
		Set<String> nonClosingSet = new HashSet<>(Arrays.asList(nonClosing));
		Set<String> skipSet = new HashSet<>(Arrays.asList("script", "style", "head", "footer", "button", "select", "meta", "link"));

		int i = 0;
		while (i < content.length()) {
			if (content.charAt(i) == '<') {
			// Find end of tag
			int end = content.indexOf('>', i);
			    if (end == -1) break;

			    String tag = content.substring(i + 1, end).trim();
			    // System.out.println("DEBUG TAG: [" + tag + "]");
			    boolean isClosing = tag.startsWith("/");

			    if (isClosing) {
				// Pop from stack
				if (!stack.isEmpty()) stack.pop();
			    } else {
				// Extract tag name (stop at space for attributes)
				String tagName = tag.split("\\s+")[0].toLowerCase().replace("/", "");
				if (skipSet.contains(tagName)) {
					boolean found = false;
					int j = end;
					while (j < content.length()) {
					    if (content.charAt(j) == '<') {
						int closeEnd = content.indexOf('>', j);
						if (closeEnd == -1) break;
						String closeTag = content.substring(j + 1, closeEnd).replaceAll("\\s+", "").toLowerCase();
						if (closeTag.equals("/" + tagName)) {
						    i = closeEnd + 1;
						    found = true;
						    break;
						}
					    }
					    j++;
					}
					if(!found) i = end + 1;
					continue;
				}
				else if (nonClosingSet.contains(tagName)) {
					System.out.println("Self-closing tag: " + tagName);
					renderTag(tagName, "", tag, container);
				} else {	
				    // Flush text buffer before pushing new tag
				    stack.push(tag); // push full tag (includes attributes)
				}
			    }

			    i = end + 1;
			} else {
			    // Collect text until next '<'
				int end = content.indexOf('<', i);
				if (end == -1) end = content.length();

				String text = content.substring(i, end).trim();
				if (!text.isEmpty() && !stack.isEmpty()) {
					String currentTag = stack.peek().split("\\s+")[0].toLowerCase();
					System.out.println(currentTag + ": " + text);
					renderTag(currentTag, text, stack.peek(), container);
				} else if (!text.isEmpty()) {
					System.out.println("text: " + text);
					renderTag("p", text, "", container);
				}

				i = end;
			}
		}
	}

	void setRequestPage(VBox root) {
		root.getChildren().clear();

		Label p = new Label("Fetching the page for you...");
		p.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(p);
	}
	
	void setInvalidURLPage(VBox root) {
		root.getChildren().clear();

		Label h1 = new Label("Invalid URL");
		Label p = new Label("The URL you entered is invalid. Check again?");
		h1.setStyle("-fx-font-size: " + H1_SIZE + "px; -fx-font-weight: bold;");

		p.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(h1);
		root.getChildren().add(p);
	}

	void setInterruptedExceptionPage(VBox root) {
		root.getChildren().clear();

		Label h1 = new Label("The thread running the request was interrupted");
		Label p = new Label("Please check application logs for more information");
		h1.setStyle("-fx-font-size: " + H1_SIZE + "px; -fx-font-weight: bold;");

		p.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(h1);
		root.getChildren().add(p);
	}

	void setIOExceptionPage(VBox root) {
		root.getChildren().clear();

		Label h1 = new Label("Network Error");
		Label p = new Label("This may be because:\n\n- The host was not found\n- The server is down\n- The server took too long to respond\n- HTTPS Certificate issues\n\nCheck your internet connection, firewall and proxy settings, and the domain name.");
		h1.setStyle("-fx-font-size: " + H1_SIZE + "px; -fx-font-weight: bold;");

		p.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(h1);
		root.getChildren().add(p);
	}

	void setPage(String content, VBox root) {
		root.getChildren().clear();

		Label p = new Label(content);
		p.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(p);
	}

	void makeSearch(String url, VBox container) {
		System.out.println("baseUrl: " + baseUrl);
		System.out.println("You searched for: " + url);
	
		// Add HTTP(s) if not present
		if (url.startsWith("//")) {
		    url = "https:" + url;
		}
		else if(url.startsWith("/")) {
			String domain = baseUrl.replaceAll("(https?://[^/]+).*", "$1");
			url = domain + url;
		}
		else if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "https://" + url;
		}
		if (!url.endsWith("/")) url = url + "/";
		baseUrl = url.substring(0, url.lastIndexOf("/") + 1);

		HttpClient client = HttpClient.newHttpClient();


		// Create the URI
		URI uri;

		try {
			uri = URI.create(url);
		} 
		// Checks for invalid characters like spaces
		catch(IllegalArgumentException e) {
			setInvalidURLPage(container);
			return;
		}

		// Create HTTP request

		HttpRequest req;

		try {
			req = HttpRequest.newBuilder()
			.header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
			.uri(uri)
			.GET()
			.build();
		} 
		// Throws if scheme doesn't exist (http(s))
		catch(IllegalArgumentException e) {
			setInvalidURLPage(container);
			return;
		}

		// Make request
		try {
			HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
			System.out.println("Status code: " + res.statusCode());
			// setPage(res.body(), container);
			if(res.statusCode() == 301) {
				String redirectUrl = res.headers().firstValue("location").orElse("");
				System.out.println("Redirecting to: " + redirectUrl);
				makeSearch(redirectUrl, container);
				return;
			} else {
				parse(res.body(), container);
				System.out.println("Successfully loaded " + url);
			}
		} catch(IOException e) {
			setIOExceptionPage(container);
			return;
		} catch(InterruptedException e) {
			setInterruptedExceptionPage(container);
			e.printStackTrace();
			return;
		}
	}

	void setHomePage(VBox root) {
		Label welcome = new Label("Welcome to Pranav's JavaFX browser!");
		Label help = new Label("Start by entering a search term in the URL bar above and pressing enter or clicking Go.");

		// Make welcome label a <h1>
		welcome.setStyle("-fx-font-size: " + H1_SIZE + "px; -fx-font-weight: bold;");

		// Make help label a <p>
		help.setStyle("-fx-font-size: " + P_SIZE + "px;");

		root.getChildren().add(welcome);
		root.getChildren().add(help);
	}

	public static void main(String args[]) {
		launch(args);
	}
}

