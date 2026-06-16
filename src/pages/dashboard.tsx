import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  FileText,
  Star,
  TrendingUp,
  Activity,
  Users,
  ArrowUpRight,
  MoreHorizontal,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

const stats = [
  {
    label: "Total Notes",
    value: "24",
    change: "+3 this week",
    icon: FileText,
    trend: "up",
  },
  {
    label: "Active Projects",
    value: "5",
    change: "2 in progress",
    icon: Activity,
    trend: "up",
  },
  {
    label: "Recent Views",
    value: "128",
    change: "+12% vs last week",
    icon: TrendingUp,
    trend: "up",
  },
  {
    label: "Collaborators",
    value: "3",
    change: "1 online now",
    icon: Users,
    trend: "neutral",
  },
];

const recentNotes = [
  {
    id: "1",
    title: "Weekly Team Meeting",
    preview: "Discussed Q4 goals, project timelines, and resource allocation...",
    date: "2 hours ago",
    tags: ["work", "meeting"],
    starred: true,
  },
  {
    id: "2",
    title: "Project Architecture Review",
    preview: "Reviewing the new microservices architecture for the platform...",
    date: "5 hours ago",
    tags: ["work", "technical"],
    starred: false,
  },
  {
    id: "3",
    title: "Book Notes: Atomic Habits",
    preview: "Key takeaways: 1% better every day, habit stacking, environment design...",
    date: "1 day ago",
    tags: ["personal", "reading"],
    starred: true,
  },
  {
    id: "4",
    title: "Product Ideas 2024",
    preview: "Brainstorming new features for the platform including AI-powered...",
    date: "2 days ago",
    tags: ["ideas"],
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
};

export function Dashboard() {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      className="mx-auto max-w-5xl space-y-8"
    >
      <motion.div variants={item}>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Welcome back</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Here's what's happening with your workspace today.
            </p>
          </div>
          <Button className="gap-2">
            <FileText className="h-4 w-4" />
            New Note
          </Button>
        </div>
      </motion.div>

      <motion.div
        variants={item}
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.label} className="relative overflow-hidden">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.label}
                </CardTitle>
                <Icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-bold">{stat.value}</span>
                  <span
                    className={cn(
                      "inline-flex items-center text-xs font-medium",
                      stat.trend === "up" && "text-green-500",
                    )}
                  >
                    <ArrowUpRight className="h-3 w-3 mr-0.5" />
                    {stat.change}
                  </span>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </motion.div>

      <motion.div variants={item}>
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Recent Notes</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-border">
              {recentNotes.map((note) => (
                <div
                  key={note.id}
                  className="flex items-start gap-4 px-6 py-4 hover:bg-accent/50 transition-colors cursor-pointer group"
                >
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                    <FileText className="h-4 w-4 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-medium truncate">{note.title}</h3>
                      {note.starred && (
                        <Star className="h-3.5 w-3.5 shrink-0 fill-yellow-500 text-yellow-500" />
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">
                      {note.preview}
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      {note.tags.map((tag) => (
                        <Badge
                          key={tag}
                          variant="outline"
                          className={cn(
                            "text-[10px] px-1.5 py-0 h-4 font-normal",
                            tagColors[tag],
                          )}
                        >
                          {tag}
                        </Badge>
                      ))}
                      <span className="text-[10px] text-muted-foreground ml-auto">
                        {note.date}
                      </span>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  );
}
