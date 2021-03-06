package softDevExam.persistence;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

import softDevExam.controller.GutenbergService;
import softDevExam.entity.*;

public class GutenbergMysql implements GutenbergService {

	private String url;
	private String username;
	private String password;
	private String driver;

	public GutenbergMysql() {
		Properties props = new Properties();

		try {
			InputStream stream = GutenbergMysql.class.getResourceAsStream("db.properties");
			props.load(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.driver = props.getProperty("MYSQL_DRIVER_CLASS");
		this.url = props.getProperty("MYSQL_URL");
		this.username = props.getProperty("MYSQL_USERNAME");
		this.password = props.getProperty("MYSQL_PASSWORD");
	}

	public GutenbergMysql(String driver, String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.driver = driver;
	}

	public Connection getConnection() throws Exception {
		Class.forName(this.driver);
		return DriverManager.getConnection(this.url, this.username, this.password);
	}

	@Override
	public List<Book> getBooksByCity(String city) throws Exception {
		Map<String, Book> bookLookup = new HashMap<>();

		try (Connection conn = getConnection()) {
			final String command = "SELECT books.*, authors.* FROM books JOIN book_author ON (book_author.bookId = books.id) "
					+ " JOIN authors ON (authors.id = book_author.authorId) "
					+ " WHERE EXISTS (SELECT 1 FROM book_city JOIN cities ON (cities.id = book_city.cityId) WHERE book_city.bookId = books.id AND cities.name = ?)";

			PreparedStatement ps = conn.prepareStatement(command);
			ps.setString(1, city);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				String bookId = rs.getString("books.id");
				Book book;

				if (bookLookup.containsKey(bookId)) {
					book = bookLookup.get(bookId);
				} else {
					book = new Book(bookId, rs.getString("books.name"), new Author(rs.getString("authors.name")));
					bookLookup.put(book.getId(), book);
				}
			}

		}

		return new ArrayList<Book>(bookLookup.values());
	}

	@Override
	public List<City> getCitiesByBook(String book) throws Exception {
		List<City> resultList = new ArrayList<>();

		try (Connection conn = getConnection()) {
			PreparedStatement ps = conn.prepareStatement(
					"SELECT cities.name, X(cities.location) as longitude, Y(cities.location) as latitude FROM books "
							+ "JOIN book_city ON (book_city.bookId = books.id) "
							+ "JOIN cities ON (cities.id = book_city.cityId) " + "WHERE books.name = ?");
			ps.setString(1, book);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				resultList.add(new City(rs.getString("name"), rs.getDouble("latitude"), rs.getDouble("longitude")));
			}
		}

		return resultList;

	}

	@Override
	public List<Book> getBooksAndCitysByAuthor(String author) throws Exception {
		List<Book> resultList = new ArrayList<>();

		Map<String, Book> bookLookup = new HashMap<>();
		try (Connection conn = getConnection()) {
			final String command = "SELECT books.*, authors.*, cities.*, X(cities.location) as longitude, Y(cities.location) as latitude FROM authors "
					+ "JOIN book_author ON (book_author.authorId = authors.id) "
					+ "JOIN books ON (books.id = book_author.bookId) "
					+ "JOIN book_city ON (book_city.bookId = books.id) "
					+ "JOIN cities ON (book_city.cityId = cities.id) " + "WHERE authors.name = ?";

			PreparedStatement ps = conn.prepareStatement(command);
			ps.setString(1, author);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String bookId = rs.getString("books.id");
				Book book;

				if (bookLookup.containsKey(bookId)) {
					book = bookLookup.get(bookId);
				} else {
					book = new Book(bookId, rs.getString("books.name"), new Author(rs.getString("authors.name")));
					bookLookup.put(book.getId(), book);
				}

				book.getCities().add(
						new City(rs.getString("cities.name"), rs.getDouble("latitude"), rs.getDouble("longitude")));
			}
		}

		return new ArrayList<Book>(bookLookup.values());

	}

	@Override
	public List<Book> getBooksByLocation(double longitude, double latitude) throws Exception {
		final int radiusInKilometers = 50;

		Map<String, Book> bookLookup = new HashMap<>();

		try (Connection conn = getConnection()) {
			// Since it not possible to fuck up numbers, we dont need to use
			// parameters.. :D
			final String command = "SELECT 	books.*, authors.*, cities.*, X(cities.location) as longitude, Y(cities.location) as latitude FROM cities "
					+ "JOIN book_city ON (book_city.cityId = cities.id) "
					+ "JOIN books ON (books.id = book_city.bookId) "
					+ "JOIN book_author ON (book_author.bookId = books.id) "
					+ "JOIN authors ON (authors.id = book_author.authorId) " + "WHERE (3959 * ACOS(COS(RADIANS("
					+ longitude + ")) * COS(RADIANS(X(cities.location))) * COS(RADIANS(Y(cities.location)) - RADIANS("
					+ latitude + ")) + SIN(RADIANS(" + longitude + ")) * SIN(RADIANS(X(cities.location))))) < "
					+ radiusInKilometers + "";

			System.out.println(command);

			Statement ps = conn.createStatement();
			ResultSet rs = ps.executeQuery(command);

			while (rs.next()) {
				String bookId = rs.getString("books.id");

				Book book;

				if (bookLookup.containsKey(bookId)) {
					book = bookLookup.get(bookId);
				} else {
					book = new Book(bookId, rs.getString("books.name"), new Author(rs.getString("authors.name")));
					bookLookup.put(book.getId(), book);
				}

				book.getCities().add(
						new City(rs.getString("cities.name"), rs.getDouble("latitude"), rs.getDouble("longitude")));
			}
		}

		return new ArrayList<Book>(bookLookup.values());
	}
}
