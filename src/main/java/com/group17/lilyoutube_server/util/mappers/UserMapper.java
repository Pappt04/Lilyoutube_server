package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.UserDTO;
import com.group17.lilyoutube_server.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "id", target = "id")
    UserDTO toDto(User user);

    @Mapping(source = "id", target = "id")
    User toEntity(UserDTO userDTO);
}
