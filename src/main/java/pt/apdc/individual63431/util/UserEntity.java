package pt.apdc.individual63431.util;

import com.google.cloud.datastore.*;

public class UserEntity {

    public static final String Kind = "User";

    public static Entity toEntity(UserRequest req, Datastore datastore) {
        Key userKey = datastore.newKeyFactory().setKind(KIND).newKey(req.username);

        Entity.Builder user = Entity.newBuilder(userKey)
                .set("email", req.email)
                .set("username", req.username)
                .set("fullName", req.fullName)
                .set("phoneNum", req.phoneNum)
                .set("password", req.password) // Encriptação recomendada
                .set("privacy", req.privacy);
        return user.build();
    }
}