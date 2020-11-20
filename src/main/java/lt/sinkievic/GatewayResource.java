package lt.sinkievic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.agroal.api.AgroalDataSource;

@Path("gw")
public class GatewayResource {
	
	private final AgroalDataSource  dataSource;
	
	public GatewayResource(AgroalDataSource  dataSource) {
		this.dataSource = dataSource;
	}

    @GET
    @Path("values")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getValues() {
    	
    	System.out.println("list values");
    	StringBuilder sb = new StringBuilder();
    	
    	try {
    		Connection connection = dataSource.getConnection();
    		PreparedStatement statement = connection.prepareStatement("select * from data_row order by id desc limit 3000");
    		try (ResultSet resultSet = statement.executeQuery()){
    			while (resultSet.next()) {
    				Double dec = resultSet.getDouble("dec_value");
    				if (resultSet.wasNull())
    					dec = null;
    				sb.append(String.format("%d %s %s %s %s %s \r\n", 
    							resultSet.getLong("id"),
    							resultSet.getString("dtime"),
    							resultSet.getString("grp"),
    							resultSet.getString("fld"),
    							dec,
    							resultSet.getString("text_value")));
    			}
    		}
    		
    	} catch (SQLException e) {
    		throw new RuntimeException(e);
    	}
    	
    	String output = sb.toString();
        return Response.status(200).entity(output).build();
    }
    
    @GET
    @Path("insert")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@QueryParam("group") String group, 
    		 					@QueryParam("field") String field, 
    		                    @QueryParam("dec") Double dec, 
    		                    @QueryParam("text") String text) {
    	
    	System.out.println(String.format("insert value(group=%s field=%s dec=%s text=%s)", group, field, dec, text));
    	if (group == null || group.isBlank())
    		return Response.status(Status.BAD_REQUEST).build();
    	if (field == null || field.isBlank())
    		return Response.status(Status.BAD_REQUEST).build();
    	
    	int rowsAffected = 0;
    	try {
    		Connection connection = dataSource.getConnection();
    		PreparedStatement sql = connection.prepareStatement("insert into data_row (dtime, grp, fld, dec_value, text_value) values (?, ?, ?, ?, ?);");
    		sql.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
//    		sql.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    		sql.setString(2, group);
    		sql.setString(3, field);
    		if (dec == null)
    			sql.setNull(4, java.sql.Types.DOUBLE);
    		else
    			sql.setDouble(4, dec);
    		sql.setString(5, text);
    		
    		
    		rowsAffected = sql.executeUpdate();
    		
    	} catch (SQLException e) {
    		throw new RuntimeException(e);
    	}
    	
    	if (rowsAffected == 1)
    		return Response.status(Status.CREATED).entity("OK").build();
    	else
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}