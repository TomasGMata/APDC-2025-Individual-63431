package pt.apdc.individual63431.util;

import com.google.cloud.datastore.*;

public class UserEntity {

    public static final String Kind = "User";

    public static Entity toEntity(UserData data, Datastore datastore) {
        Key userKey = datastore.newKeyFactory().setKind(Kind).newKey(data.username);

        Entity.Builder user = Entity.newBuilder(userKey)
                .set("email", data.email)
                .set("username", data.username)
                .set("fullName", data.fullName)
                .set("password", data.password)
                .set("phoneNum", data.phoneNumber)
                .set("privacy", data.privacy)
                .set("role", data.role)     //ENDUSER, BACKOFFICE, ADMIN, PARTNER
                .set("state", data.state);
        return user.build();
    }
    
}