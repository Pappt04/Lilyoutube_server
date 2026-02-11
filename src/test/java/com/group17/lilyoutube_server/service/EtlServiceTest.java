package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.model.PopularVideo;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.PostView;
import com.group17.lilyoutube_server.repository.PopularVideoRepository;
import com.group17.lilyoutube_server.repository.PostViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EtlServiceTest {

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PopularVideoRepository popularVideoRepository;

    @InjectMocks
    private EtlService etlService;

    @Test
    public void testRunPipeline() {
        LocalDateTime now = LocalDateTime.now();
        
        Post p1 = new Post(); p1.setId(1L);
        Post p2 = new Post(); p2.setId(2L);
        Post p3 = new Post(); p3.setId(3L);
        Post p4 = new Post(); p4.setId(4L);

        // Views
        // P1: Today (~0 days ago, weight 8) + Yesterday (1 day ago, weight 7) = 15
        PostView v1_today = new PostView(); v1_today.setPost(p1); v1_today.setCreatedAt(now);
        PostView v1_ystr = new PostView(); v1_ystr.setPost(p1); v1_ystr.setCreatedAt(now.minusDays(1));

        // P2: 2 days ago (weight 6) + 3 days ago (weight 5) = 11
        PostView v2_2d = new PostView(); v2_2d.setPost(p2); v2_2d.setCreatedAt(now.minusDays(2));
        PostView v2_3d = new PostView(); v2_3d.setPost(p2); v2_3d.setCreatedAt(now.minusDays(3));

        // P3: 7 days ago (weight 1)
        PostView v3_7d = new PostView(); v3_7d.setPost(p3); v3_7d.setCreatedAt(now.minusDays(7));
        
        // P4: 6 days ago (weight 2)
        PostView v4_6d = new PostView(); v4_6d.setPost(p4); v4_6d.setCreatedAt(now.minusDays(6));


        when(postViewRepository.findAllByCreatedAtAfter(any())).thenReturn(
                Arrays.asList(v1_today, v1_ystr, v2_2d, v2_3d, v3_7d, v4_6d)
        );

        etlService.runPipeline();

        ArgumentCaptor<List<PopularVideo>> captor = ArgumentCaptor.forClass(List.class);
        verify(popularVideoRepository).saveAll(captor.capture());

        List<PopularVideo> saved = captor.getValue();
        assertEquals(3, saved.size());
        
        // Check order
        assertEquals(1L, saved.get(0).getPost().getId()); // P1
        assertEquals(15.0, saved.get(0).getScore(), 0.01);
        
        assertEquals(2L, saved.get(1).getPost().getId()); // P2
        assertEquals(11.0, saved.get(1).getScore(), 0.01);

        assertEquals(4L, saved.get(2).getPost().getId()); // P4
        assertEquals(2.0, saved.get(2).getScore(), 0.01);
    }
}
