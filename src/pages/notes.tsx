import { useState } from "react";
import { useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { NoteEditor } from "./note-editor";
import {
  Search,
  FileText,
  Plus,
  MoreHorizontal,
  Star,
  FolderOpen,
} from "lucide-react";

const notesList = [
  {
    id: "1",
    title: "Weekly Team Meeting",
    preview: "Discussed Q4 goals, project timelines...",
    date: "2h ago",
    tags: ["work", "meeting"],
    starred: true,
  },
  {
    id: "2",
    title: "Project Architecture Review",
    preview: "Reviewing microservices architecture...",
    date: "5h ago",
    tags: ["work", "technical"],
    starred: false,
  },
  {
    id: "3",
    title: "Book Notes: Atomic Habits",
    preview: "Key takeaways from James Clear's book...",
    date: "1d ago",
    tags: ["personal", "reading"],
    starred: true,
  },
  {
    id: "4",
    title: "Product Ideas 2024",
    preview: "Brainstorming new features...",
    date: "2d ago",
    tags: ["ideas"],
    starred: false,
  },
  {
    id: "5",
    title: "Design System Guidelines",
    preview: "Components, tokens, and patterns...",
    date: "3d ago",
    tags: ["work", "design"],
    starred: false,
  },
];

const tagColors: Record<string, string> = {
  work: "bg-blue-500/10 text-blue-500 border-blue-500/20",
  meeting: "bg-green-500/10 text-green-500 border-green-500/20",
  technical: "bg-purple-500/10 text-purple-500 border-purple-500/20",
  personal: "bg-yellow-500/10 text-yellow-500 border-yellow-500/20",
  reading: "bg-pink-500/10 text-pink-500 border-pink-500/20",
  ideas: "bg-orange-500/10 text-orange-500 border-orange-500/20",
  design: "bg-teal-500/10 text-teal-500 border-teal-500/20",
};

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.05 },
  },
};

const listItem = {
  hidden: { opacity: 0, x: -10 },
  show: { opacity: 1, x: 0 },
};

export function Notes() {
  const { id } = useParams();
  const [searchQuery, setSearchQuery] = useState("");

  const filteredNotes = notesList.filter(
    (note) =>
      note.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      note.tags.some((t) => t.includes(searchQuery.toLowerCase())),
  );

  return (
    <div className="flex h-full -m-6">
      <div className="flex w-72 flex-shrink-0 flex-col border-r border-border bg-sidebar">
        <div className="flex items-center justify-between p-4 pb-2">
          <h2 className="text-sm font-semibold">Notes</h2>
          <Button size="sm" className="h-7 gap-1 text-xs">
            <Plus className="h-3.5 w-3.5" />
            New
          </Button>
        </div>

        <div className="px-4 pb-3">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search notes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="h-8 pl-8 text-xs bg-muted/50"
            />
          </div>
        </div>

        <ScrollArea className="flex-1 px-2">
          <motion.div
            variants={container}
            initial="hidden"
            animate="show"
            className="space-y-0.5"
          >
            {filteredNotes.map((note) => (
              <motion.button
                key={note.id}
                variants={listItem}
                className={cn(
                  "w-full text-left rounded-lg px-3 py-2 transition-colors group",
                  id === note.id
                    ? "bg-accent"
                    : "hover:bg-accent/50",
                )}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <FileText className="h-3.5 w-3.5 shrink-0 text-muted-foreground mt-0.5" />
                    <span className="text-xs font-medium truncate">
                      {note.title}
                    </span>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {note.starred && (
                      <Star className="h-3 w-3 fill-yellow-500 text-yellow-500" />
                    )}
                    <MoreHorizontal className="h-3 w-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                </div>
                <p className="text-[11px] text-muted-foreground mt-0.5 line-clamp-1 pl-[22px]">
                  {note.preview}
                </p>
                <div className="flex items-center gap-1.5 mt-1.5 pl-[22px]">
                  {note.tags.map((tag) => (
                    <Badge
                      key={tag}
                      variant="outline"
                      className={cn(
                        "text-[9px] px-1 py-0 h-3.5 font-normal",
                        tagColors[tag],
                      )}
                    >
                      {tag}
                    </Badge>
                  ))}
                  <span className="text-[9px] text-muted-foreground ml-auto">
                    {note.date}
                  </span>
                </div>
              </motion.button>
            ))}
          </motion.div>
        </ScrollArea>
      </div>

      <div className="flex-1">
        {id ? (
          <NoteEditor noteId={id} />
        ) : (
          <div className="flex h-full items-center justify-center">
            <div className="text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-muted">
                <FolderOpen className="h-6 w-6 text-muted-foreground" />
              </div>
              <h3 className="mt-4 text-sm font-medium">No note selected</h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Choose a note from the sidebar or create a new one
              </p>
              <Button className="mt-4 gap-2" size="sm">
                <Plus className="h-4 w-4" />
                Create Note
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
