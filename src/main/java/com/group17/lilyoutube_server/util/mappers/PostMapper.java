package com.group17.lilyoutube_server.util.mappers;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostDTO toDto(Post post);

    Post toEntity(PostDTO postDTO);
}
